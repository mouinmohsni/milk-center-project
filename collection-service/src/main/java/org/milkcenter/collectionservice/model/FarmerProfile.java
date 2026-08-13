package org.milkcenter.collectionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "farmers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Lombok: permet de construire des objets de manière fluide
public class FarmerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;


    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column( name = "farmName", nullable = false)
    private String farmName;

    @Column( name = "address", nullable = false)
    private String address;

    @Column( name = "latitude", nullable = false)
    private Double latitude;

    @Column( name = "longitude", nullable = false)
    private Double longitude;

    @Column( name = "herdSize")
    private Integer  herdSize;

    @Column( name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date createdAt = new java.util.Date();

    @Column(name = "updatedAt", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updatedAt = new java.util.Date();


}
