package org.milkcenter.fleetservice.model;

import jakarta.persistence.*;
import lombok.*;

import org.milkcenter.fleetservice.enums.VehicleStatus;

import java.util.Date;

@Entity
@Data
@Table(name = "vehicle")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {


    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(name = "license_plate",unique = true, nullable = false)
    private String licensePlate ;

    @Column(name = "model")
    private String model ;

    @Column(name = "capacity")
    private Double capacity ;

    @Column(name = "km")
    private Long km ;

    @Column(name = "last_oil_change_mileage")
    private Long lastOilChangeMileage  ;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VehicleStatus status = VehicleStatus.READY;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt = new Date();



    @PrePersist
    public void prePersist() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }
}
