package org.milkcenter.collectionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.milkcenter.collectionservice.enums.CollectionStatus;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "milk_collections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(name = "driver_user_id")
    private Long driverUserId;

    @Column(name = "route_stop_id")
    private Long routeStopId;

    @Column(name = "collected_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date collectedAt;

    @Column(name = "quantity_liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityLiters;

    @Column(name = "temperature_celsius", precision = 4, scale = 2)
    private BigDecimal temperatureCelsius;

    @Column(name = "quality_notes", length = 500)
    private String qualityNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CollectionStatus status = CollectionStatus.PENDING;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "correction_count")
    private Integer correctionCount = 0;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "validator_user_id")
    private Long validatorUserId;

    @Column(name = "validation_notes", length = 500)
    private String validationNotes;

    @Column(name = "idempotency_key", unique = true, nullable = false, length = 64)
    private String idempotencyKey;

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
