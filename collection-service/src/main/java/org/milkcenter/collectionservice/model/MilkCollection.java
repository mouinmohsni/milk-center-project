package org.milkcenter.collectionservice.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    // Références logiques (pas de FK vers Identity ou Fleet & Ops)
    @Column(name = "farmer_id", nullable = false)
    private Long farmerId; // Référence vers FarmerProfile (même service, mais gardée simple)

    @Column(name = "driver_user_id")
    private Long driverUserId; // Référence logique vers Identity (le chauffeur)

    @Column(name = "route_stop_id")
    private Long routeStopId; // Référence logique vers Fleet & Ops (future entité RouteStop)

    // Données métier de la collecte
    @Column(name = "collected_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date collectedAt;

    @Column(name = "quantity_liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityLiters; // Quantité mesurée en litres

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CollectionStatus status = CollectionStatus.PENDING;

    @Column(name = "notes", length = 500)
    private String notes; // Observations du chauffeur (qualité visuelle, odeur, etc.)

    @Column(name = "correction_count")
    private Integer  correctionCount = 0 ;

    @Column(name = "updatedByUserId" )
    private Long updatedByUserId ;

    // Clé d'unicité pour éviter les doublons lors de synchronisations mobiles
    @Column(name = "idempotency_key", unique = true, nullable = false, length = 64)
    private String idempotencyKey;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt = new Date();



    // Méthodes métier
    public void accept() {
        this.status = CollectionStatus.ACCEPTED;
    }

    public void reject(String reason) {
        this.status = CollectionStatus.REJECTED;
        this.notes = (this.notes == null ? "" : this.notes) + " [Rejet: " + reason + "]";
    }

    public void correctQuantity(BigDecimal newQuantity) {
        this.quantityLiters = newQuantity;
        this.status = CollectionStatus.CORRECTED;
    }

    @PrePersist
    public void prePersist() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

}
