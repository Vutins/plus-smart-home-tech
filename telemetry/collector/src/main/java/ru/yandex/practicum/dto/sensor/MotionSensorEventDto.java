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
public class MotionSensorEventDto extends SensorEventDto {

    Integer linkQuality;
    Boolean motion;
    Integer voltage;

    @Override
    public SensorTypeAvro getType() {
        return SensorTypeAvro.MOTION_SENSOR_EVENT;
    }
}
