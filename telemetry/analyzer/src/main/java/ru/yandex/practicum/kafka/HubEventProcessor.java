package ru.yandex.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.service.HubEventService;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final HubEventService service;
    private final AnalyzerConsumerConfig consumerConfig;

    @Value("${spring.kafka.topics.hub-topic-name}")
    private String hubEventTopic;

    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public void run() {
        log.info("🎯 HubEventProcessor RUN! Топик: {}, Group: {}",
                hubEventTopic, consumerConfig.getConsumerProperties().get("group.id"));

        try (KafkaConsumer<String, HubEventAvro> consumer = consumerConfig.createHubEventConsumer()) {
            consumer.subscribe(Collections.singletonList(hubEventTopic));

            while (running.get()) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(100));
                log.info("📨 HUB EVENTS: {} записей из {}", records.count(), hubEventTopic);

                if (!records.isEmpty()) {
                    service.saveHubEvent(records);
                    consumer.commitSync();
                    log.info("✅ {} событий обработано", records.count());
                }
            }
        } catch (WakeupException ignored) {
            log.info("🛑 HubEventProcessor остановлен по WakeupException");
        } catch (Exception e) {
            log.error("💥 ОШИБКА HubEventProcessor!", e);
        }

        log.info("👋 HubEventProcessor завершил работу");
    }

    public void shutdown() {
        log.info("🔻 Запрошена остановка HubEventProcessor");
        running.set(false);
    }
}