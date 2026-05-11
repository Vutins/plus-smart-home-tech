package ru.yandex.practicum.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.config.KafkaConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class KafkaEventConsumer implements AutoCloseable {
    private final KafkaConsumer<String, SpecificRecordBase> consumer;

    public KafkaEventConsumer(KafkaConfig kafkaConfig) {
        this.consumer = new KafkaConsumer<>(kafkaConfig.getConsumerConfig());
    }

    @PostConstruct
    public void init() {
        List<String> topics = Arrays.asList("telemetry.sensors.v1", "telemetry.hub.v1");
        consumer.subscribe(topics);
        log.info("Консюмер подписан на топики: {}", topics);
    }

    public void consume() {
        try {
            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    String eventClass = record.value().getClass().getSimpleName();
                    log.info("Получено сообщение из топика {}: {} (партиция: {}, смещение: {})",
                            record.topic(), record.value(), record.partition(), record.offset());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при потреблении сообщений из Kafka", e);
        }
    }

    @Override
    @PreDestroy
    public void close() {
        log.info("Закрытие Kafka Consumer...");
        consumer.close(Duration.ofSeconds(10));
        log.info("Kafka Consumer успешно закрыт");
    }
}