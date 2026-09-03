package org.milkcenter.fleetservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "fuel_consumption")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelConsumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    // Un plein peut couvrir plusieurs exécutions de tournées
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "fuel_consumption_executions",
            joinColumns = @JoinColumn(name = "fuel_consumption_id"),
            inverseJoinColumns = @JoinColumn(name = "route_execution_id")
    )
    private List<RouteExecution> routeExecutions;

    @Column(name = "fuel_type", nullable = false)
    private String fuelType; // ex: "Gazoil sans plomb", "Diesel"

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fuel_date", nullable = false)
    private Date fuelDate;

    @Column(name = "odometer", nullable = false)
    private Long odometer; // Kilométrage au moment du plein

    @Column(name = "liters", precision = 7, scale = 2, nullable = false)
    private BigDecimal liters;

    @Column(name = "price_per_liter", precision = 7, scale = 3, nullable = false)
    private BigDecimal pricePerLiter;

    @Column(name = "total_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalCost;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Date createdAt = new Date();

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        if (totalCost == null && liters != null && pricePerLiter != null) {
            totalCost = liters.multiply(pricePerLiter);
        }
    }
}
