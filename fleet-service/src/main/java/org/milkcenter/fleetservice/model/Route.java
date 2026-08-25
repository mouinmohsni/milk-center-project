package org.milkcenter.fleetservice.model;


import jakarta.persistence.*;
import lombok.*;
import org.milkcenter.fleetservice.enums.RouteStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@Table(name = "route")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(name = "name")
    private String name ;

    // Remplacez Long driverId par :
    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    // Remplacez Long vehicleId par :
    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;


    @Column(name = "planned_date")
    private Date plannedDate ;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RouteStatus status = RouteStatus.ACTIVE ;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt = new Date();

    @ToString.Exclude
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC") // Les arrêts seront toujours triés !
    private List<RouteStop> stops = new ArrayList<>();



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
