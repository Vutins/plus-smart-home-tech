package ru.yandex.practicum.kafka;

import lombok.Getter;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.stereotype.Component;
import ru.practicum.serializer.GeneralAvroSerializer;

import java.util.Properties;

@Getter
@Component
public class KafkaConfig {

    private Properties producerConfig;

    public void init() {
        this.producerConfig = new Properties();
        this.producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        this.producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        this.producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class.getName());
        this.producerConfig.put(ProducerConfig.ACKS_CONFIG, "1");
        this.producerConfig.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        this.producerConfig.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        this.producerConfig.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    }
}
