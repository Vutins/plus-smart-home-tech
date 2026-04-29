package ru.yandex.practicum.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.config.KafkaProducerProperties;
import ru.yandex.practicum.kafka.config.TopicType;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@Component
public class KafkaEventProducer implements AutoCloseable {

    private final KafkaProducer<String, SpecificRecordBase> producer;
    private final KafkaProducerProperties properties;

    public KafkaEventProducer(KafkaProducerProperties properties) {
        this.properties = properties;
        this.producer = new KafkaProducer<>(createProducerConfig());
        log.info("Kafka producer initialized with config: {}", createProducerConfig());
    }

    private Map<String, Object> createProducerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientId());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, properties.getKeySerializer());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, properties.getValueSerializer());
        config.put(ProducerConfig.ACKS_CONFIG, properties.getAcks());

        if (properties.getLingerMs() != null) {
            config.put(ProducerConfig.LINGER_MS_CONFIG, properties.getLingerMs());
        }
        if (properties.getBatchSize() != null) {
            config.put(ProducerConfig.BATCH_SIZE_CONFIG, properties.getBatchSize());
        }
        if (properties.getBufferMemory() != null) {
            config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, properties.getBufferMemory());
        }

        return config;
    }

    public void send(SpecificRecordBase event, String hubId, Instant timestamp, TopicType topicType) {
        String topic = resolveTopic(topicType);
        String eventClass = event.getClass().getSimpleName();

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                timestamp.toEpochMilli(),
                hubId,
                event
        );

        log.trace("Сохраняю событие {} связанное с хабом {} в топик {}",
                eventClass, hubId, topic);

        Future<RecordMetadata> futureResult = producer.send(record);
        producer.flush(); // ⚠️ Оставляем явный flush, как в исходной логике

        try {
            RecordMetadata metadata = futureResult.get();
            log.info("Событие {} было успешно сохранено в топик {} в партицию {} со смещением {}",
                    eventClass, metadata.topic(), metadata.partition(), metadata.offset());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Не удалось записать событие {} в топик {}", eventClass, topic, e);
        }
    }

    private String resolveTopic(TopicType topicType) {
        if (properties.getTopics() == null) {
            return topicType.getTopic();
        }
        return properties.getTopics().getOrDefault(topicType.name().toLowerCase(), topicType.getTopic());
    }

    @Override
    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}