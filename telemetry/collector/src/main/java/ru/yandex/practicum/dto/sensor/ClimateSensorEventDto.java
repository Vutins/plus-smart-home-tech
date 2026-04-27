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
public class ClimateSensorEventDto extends SensorEventDto {

    Integer temperatureC;
    Integer humidity;
    Integer co2Level;

    @Override
    public SensorTypeAvro getType() {
        return SensorTypeAvro.CLIMATE_SENSOR_EVENT;
    }
}
