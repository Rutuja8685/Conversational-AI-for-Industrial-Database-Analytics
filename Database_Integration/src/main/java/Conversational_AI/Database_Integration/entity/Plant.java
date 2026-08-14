package Conversational_AI.Database_Integration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Plant")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plant {

    @Id
    @Column(name = "plant_id")
    private String plantId;

    @Column(name = "plant_name", nullable = false)
    private String plantName;

    @Column(name = "location")
    private String location;

    @Column(name = "status")
    private String status;
}