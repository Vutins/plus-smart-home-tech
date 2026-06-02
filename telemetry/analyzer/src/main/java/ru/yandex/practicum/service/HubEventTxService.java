package ru.yandex.practicum.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.repository.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventTxService {

    private final ActionRepository actionRepository;
    private final ConditionRepository conditionRepository;
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;

    @Transactional
    public void saveDevice(String sensorId, String hubId) {
        log.info("🔧 saveDevice: sensorId={}, hubId={}", sensorId, hubId);

        Sensor sensor = Sensor.builder()
                .id(sensorId)
                .hubId(hubId)
                .build();

        Sensor saved = sensorRepository.save(sensor);
        log.info("✅ Сенсор сохранён: id={}, hubId={}", saved.getId(), saved.getHubId());
    }

    @Transactional
    public Scenario saveScenario(ScenarioAddedEventAvro event, String hubId) {
        log.info("🔧 saveScenario: name='{}', hubId={}, conditions={}, actions={}",
                event.getName(), hubId, event.getConditions().size(), event.getActions().size());

        //Собираем набор по условиям и действиям
        Set<String> sensors = new HashSet<>();
        event.getConditions().forEach(condition -> sensors.add(condition.getSensorId()));
        event.getActions().forEach(action -> sensors.add(action.getSensorId()));
        log.debug("📋 Сенсоры для проверки: {}", sensors);

        //проверяем, что всё есть, можно дальше работать.
        boolean allSensorsExists = sensorRepository.existsByIdInAndHubId(sensors, hubId);
        log.info("✅ Сенсоры существуют для hubId={}: {}", hubId, allSensorsExists);

        if(!allSensorsExists) {
            log.error("❌ ОШИБКА: Нет сенсоров для сценария. hubId={}, sensors={}", hubId, sensors);
            throw new IllegalStateException("Нет возможности создать сценарий с использованием неизвестного устройства");
        }

        //Пытаемся найти уже существующий сценарий.
        Optional<Scenario> maybeExist = scenarioRepository.findByHubIdAndName(hubId, event.getName());
        log.info("🔍 Существующий сценарий hubId={}, name={}: {}", hubId, event.getName(),
                maybeExist.isPresent() ? "НАЙДЕН" : "ОТСУТСТВУЕТ");

        Scenario scenario;
        if(maybeExist.isEmpty()) {
            scenario = new Scenario();
            scenario.setName(event.getName());
            scenario.setHubId(hubId);
            log.info("➕ Создаём новый сценарий: {}", event.getName());
        } else {
            scenario = maybeExist.get();
            log.info("🔄 Обновляем существующий сценарий: id={}", scenario.getId());

            // Удаляем старые условия и действия
            Map<String, Condition> conditions = scenario.getConditions();
            if (!conditions.isEmpty()) {
                conditionRepository.deleteAll(conditions.values());
                log.info("🗑️ Удалено условий: {}", conditions.size());
                scenario.getConditions().clear();
            }

            Map<String, Action> actions = scenario.getActions();
            if (!actions.isEmpty()) {
                actionRepository.deleteAll(actions.values());
                log.info("🗑️ Удалено действий: {}", actions.size());
                scenario.getActions().clear();
            }
        }

        //Заново пересобираем новые условия
        int conditionCount = 0;
        for (ScenarioConditionAvro eventCondition : event.getConditions()) {
            Condition condition = new Condition();
            condition.setType(eventCondition.getType().toString());
            condition.setOperation(setStringOperation(eventCondition.getOperation()));
            condition.setValue(mapValue(eventCondition.getValue()));

            scenario.addCondition(eventCondition.getSensorId(), condition);
            conditionCount++;
            log.debug("➕ Условие: sensorId={}, type={}, operation={}, value={}",
                    eventCondition.getSensorId(), condition.getType(), condition.getOperation(), condition.getValue());
        }
        log.info("✅ Добавлено условий: {}", conditionCount);

        //Заново пересобираем новые действия
        int actionCount = 0;
        for (DeviceActionAvro eventAction : event.getActions()) {
            Action action = new Action();
            action.setType(eventAction.getType().toString());
            if (eventAction.getType().equals(ActionTypeAvro.SET_VALUE)) {
                action.setValue(mapValue(eventAction.getValue()));
            } else {
                action.setValue(0); // или 1, если тебе так логичнее
            }

            scenario.addAction(eventAction.getSensorId(), action);
            actionCount++;
            log.debug("➕ Действие: sensorId={}, type={}, value={}",
                    eventAction.getSensorId(), action.getType(), action.getValue());
        }
        log.info("✅ Добавлено действий: {}", actionCount);

        // Сохраняем условия и действия
        conditionRepository.saveAll(scenario.getConditions().values());
        actionRepository.saveAll(scenario.getActions().values());

        Scenario savedScenario = scenarioRepository.save(scenario);

        // 2. Строим связи scenario_conditions
        for (Map.Entry<String, Condition> entry : scenario.getConditions().entrySet()) {
            String sensorId = entry.getKey();
            Condition condition = entry.getValue();

            Sensor sensor = sensorRepository.findByIdAndHubId(sensorId, hubId)
                    .orElseThrow(() -> new EntityNotFoundException("Сенсор " + sensorId + " не найден при связывании условия"));

            ScenarioConditionId id = ScenarioConditionId.builder()
                    .scenarioId(savedScenario.getId())
                    .sensorId(sensor.getId())
                    .conditionId(condition.getId())
                    .build();

            ScenarioCondition sc = ScenarioCondition.builder()
                    .id(id)
                    .scenario(savedScenario)
                    .sensor(sensor)
                    .condition(condition)
                    .build();

            scenarioConditionRepository.save(sc);
        }

        // 3. Строим связи scenario_actions
        for (Map.Entry<String, Action> entry : scenario.getActions().entrySet()) {
            String sensorId = entry.getKey();
            Action action = entry.getValue();

            Sensor sensor = sensorRepository.findByIdAndHubId(sensorId, hubId)
                    .orElseThrow(() -> new EntityNotFoundException("Сенсор " + sensorId + " не найден при связывании действия"));

            ScenarioActionId id = ScenarioActionId.builder()
                    .scenarioId(savedScenario.getId())
                    .sensorId(sensor.getId())
                    .actionId(action.getId())
                    .build();

            ScenarioAction sa = ScenarioAction.builder()
                    .id(id)
                    .scenario(savedScenario)
                    .sensor(sensor)
                    .action(action)
                    .build();

            scenarioActionRepository.save(sa);
        }

        log.info("🎉 СЦЕНАРИЙ СОХРАНЁН: id={}, name='{}', hubId={}, conditions={}, actions={}",
                savedScenario.getId(), savedScenario.getName(), savedScenario.getHubId(),
                scenario.getConditions().size(), scenario.getActions().size());

        return savedScenario;
    }

    @Transactional
    public void removeDevice(String sensorId, String hubId) {
        log.info("🗑️ removeDevice: sensorId={}, hubId={}", sensorId, hubId);
        Sensor sensor = sensorRepository.findByIdAndHubId(sensorId, hubId)
                .orElseThrow(() -> new EntityNotFoundException("Сенсор " + sensorId + " не найден"));
        sensorRepository.delete(sensor);
        log.info("✅ Сенсор удалён: {}", sensorId);
    }

    @Transactional
    public void removeScenario(String hubId, String name) {
        log.info("🗑️ removeScenario: hubId={}, name={}", hubId, name);
        Scenario scenario = scenarioRepository.findByHubIdAndName(name, hubId)
                .orElseThrow(() -> new EntityNotFoundException("Сценарий " + name + " не найден"));
        scenarioRepository.delete(scenario);
        log.info("✅ Сценарий удалён: {}", name);
    }

    private String setStringOperation(ConditionOperationAvro operationType) {
        return switch (operationType) {
            case EQUALS -> "EQUALS";
            case GREATER_THAN -> "GREATER_THAN";
            case LOWER_THAN -> "LOWER_THAN";
        };
    }

    private Integer mapValue(Object rawValue) {
        return switch (rawValue) {
            case Boolean b -> b ? 1 : 0;
            case Integer i -> i;
            case null -> 0;
            default -> 0;
        };
    }
}