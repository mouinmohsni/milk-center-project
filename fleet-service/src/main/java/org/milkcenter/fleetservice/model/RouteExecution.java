package org.milkcenter.fleetservice.model;


import jakarta.persistence.*;
import lombok.*;
import org.milkcenter.fleetservice.enums.RouteExecutionStatus;

import java.util.Date;

@Entity
@Table(
        name = "route_execution",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_route_execution_date",
                columnNames = {"route_id", "execution_date"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actual_driver_id", nullable = false)
    private Driver actualDriver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actual_vehicle_id", nullable = false)
    private Vehicle actualVehicle;

    @Temporal(TemporalType.DATE)
    @Column(name = "execution_date", nullable = false)
    private Date executionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RouteExecutionStatus status = RouteExecutionStatus.PLANNED;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "started_at")
    private Date startedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "finished_at")
    private Date finishedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Date updatedAt = new Date();

    @PrePersist
    public void prePersist() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = RouteExecutionStatus.PLANNED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }
}
