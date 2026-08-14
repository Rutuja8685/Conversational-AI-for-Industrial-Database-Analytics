package Conversational_AI.Database_Integration.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Sensor")
@Data
public class Sensor {
    @Id
    @Column(name = "sensor_id")
    private String sensorId;

    @Column(name = "sensor_name")
    private String sensorName;

    @Column(name = "sensor_type")
    private String sensorType;

    private String status;
}