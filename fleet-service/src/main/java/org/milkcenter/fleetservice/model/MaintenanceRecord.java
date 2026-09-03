package org.milkcenter.fleetservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.milkcenter.fleetservice.enums.MaintenanceStatus;
import org.milkcenter.fleetservice.enums.MaintenanceType;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "maintenance_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false)
    private MaintenanceType maintenanceType;

    @Column(name = "description", length = 500)
    private String description;

    @Temporal(TemporalType.DATE)
    @Column(name = "maintenance_date", nullable = false)
    private Date maintenanceDate;

    // Ces champs peuvent être null au début (IN_PROGRESS) et remplis à la fin (COMPLETED)
    @Column(name = "odometer")
    private Long odometer;

    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "provider")
    private String provider;

    @Column(name = "next_maintenance_odometer")
    private Long nextMaintenanceOdometer;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Date createdAt = new Date();

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
