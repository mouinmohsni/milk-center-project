package org.milkcenter.fleetservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.milkcenter.fleetservice.enums.DriverStatus;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "driver")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(name = "user_id", nullable=false)
    private Long userId ;

    @Column(name = "license_number")
    private String licenseNumber ;

    @Column(name = "salary")
    private BigDecimal salary ;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DriverStatus status = DriverStatus.AVAILABLE ;

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
