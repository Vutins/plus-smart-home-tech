package ru.yandex.practicum.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.config.KafkaConfig;
import ru.yandex.practicum.kafka.config.TopicType;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@Component
public class KafkaEventProducer implements AutoCloseable {

    private final KafkaProducer<String, SpecificRecordBase> producer;

    public KafkaEventProducer(KafkaConfig kafkaConfig) {
        log.info("Kafka configuration: {}", kafkaConfig.getProducerConfig());
        this.producer = new KafkaProducer<>(kafkaConfig.getProducerConfig());
    }

    public void send(SpecificRecordBase event, String hubId, Instant timestamp, TopicType topicType) {
        String topic = topicType.getTopic();

        String eventClass = event.getClass().getSimpleName();

//        long ts = timestamp != null ? timestamp.toEpochMilli() : System.currentTimeMillis();


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
        producer.flush();
        try {
            RecordMetadata metadata = futureResult.get();
            log.info("Событие {} было успешно сохранёно в топик {} в партицию {} со смещением {}",
                    eventClass, metadata.topic(), metadata.partition(), metadata.offset());
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Не удалось записать событие {} в топик {}", eventClass, topic, e);
        }
    }

    @Override
    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}