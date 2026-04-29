package ru.yandex.practicum.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collector.kafka.producer")
@Validated
public class KafkaProducerProperties {

    @NotBlank
    private String bootstrapServers;

    private String clientId;
    private String keySerializer;

    @NotBlank
    private String valueSerializer;

    @NotNull
    private String acks;

    private Integer lingerMs;
    private Integer batchSize;
    private Long bufferMemory;

    private Map<String, String> topics;
}