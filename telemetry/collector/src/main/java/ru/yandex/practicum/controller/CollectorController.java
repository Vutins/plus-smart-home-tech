package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.yandex.practicum.dto.hub.HubEventDto;
import ru.yandex.practicum.dto.sensor.SensorEventDto;
import ru.yandex.practicum.service.CollectorEventService;

@Slf4j
@RequestMapping("/events")
@RequiredArgsConstructor
public class CollectorController {

    private final CollectorEventService collectorEventService;

    @PostMapping("/sensors")
    public ResponseEntity<Void> collectSensorEvent(
            @Valid @RequestBody SensorEventDto sensorEventDto) {

        log.info("Получено событие от датчика: type={}, id={}, hubId={}",
                sensorEventDto.getType(),
                sensorEventDto.getId(),
                sensorEventDto.getHubId());

        collectorEventService.processSensorEvent(sensorEventDto);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/hubs")
    public ResponseEntity<Void> collectHubEvent(
            @Valid @RequestBody HubEventDto hubEventDto) {

        log.info("Получено событие от хаба: type={}, hubId={}",
                hubEventDto.getType(),
                hubEventDto.getHubId());

        collectorEventService.processHubEvent(hubEventDto);

        return ResponseEntity.ok().build();
    }
}