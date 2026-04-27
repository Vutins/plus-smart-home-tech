package ru.yandex.practicum.dto.sensor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.kafka.telemetry.event.SensorTypeAvro;

@Getter
@Setter
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemperatureSensorEventDto extends SensorEventDto {

    Integer temperatureC;
    Integer temperatureF;

    @Override
    public SensorTypeAvro getType() {
        return SensorTypeAvro.TEMPERATURE_SENSOR_EVENT;
    }
}
