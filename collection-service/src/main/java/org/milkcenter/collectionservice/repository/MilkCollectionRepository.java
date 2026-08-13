package org.milkcenter.collectionservice.repository;

import org.milkcenter.collectionservice.enums.CollectionStatus;
import org.milkcenter.collectionservice.model.MilkCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface MilkCollectionRepository extends JpaRepository<MilkCollection, Long> {

    // ============================================
    // Requêtes dérivées (Spring génère le SQL)
    // ============================================

    // Toutes les collectes d'un agriculteur
    List<MilkCollection> findByFarmerId(Long farmerId);

    // Toutes les collectes d'un chauffeur
    List<MilkCollection> findByDriverUserId(Long driverUserId);

    // Toutes les collectes d'un arrêt de tournée
    List<MilkCollection> findByRouteStopId(Long routeStopId);

    // Collectes d'un agriculteur avec un statut précis
    List<MilkCollection> findByFarmerIdAndStatus(Long farmerId, CollectionStatus status);

    // Vérifier si une collecte existe déjà (idempotence)
    boolean existsByIdempotencyKey(String idempotencyKey);

    // Collectes entre deux dates
    List<MilkCollection> findByCollectedAtBetween(Date start, Date end);

    // ============================================
    // Requêtes JPQL personnalisées
    // ============================================

    // Première collecte d'un agriculteur dépassant une quantité (triée par date descendante)
    @Query("SELECT mc FROM MilkCollection mc WHERE mc.farmerId = :farmerId AND mc.quantityLiters > :quantityLiters ORDER BY mc.collectedAt DESC")
    Optional<MilkCollection> findFirstBySupQuantityLiters(
            @Param("farmerId") Long farmerId,
            @Param("quantityLiters") BigDecimal quantityLiters
    );

    // Total de litres pour un agriculteur sur une période avec un statut donné
    @Query("SELECT SUM(mc.quantityLiters) FROM MilkCollection mc WHERE mc.farmerId = :farmerId AND mc.status = :status AND mc.collectedAt BETWEEN :start AND :end")
    BigDecimal sumQuantityByFarmerIdAndStatusAndPeriod(
            @Param("farmerId") Long farmerId,
            @Param("status") CollectionStatus status,
            @Param("start") Date start,
            @Param("end") Date end
    );

    // Total de litres ACCEPTED pour un agriculteur (tout temps)
    @Query("SELECT SUM(mc.quantityLiters) FROM MilkCollection mc WHERE mc.farmerId = :farmerId AND mc.status = 'ACCEPTED'")
    BigDecimal sumAcceptedQuantityByFarmerId(@Param("farmerId") Long farmerId);

    // Compter les collectes par statut pour un agriculteur
    @Query("SELECT mc.status, COUNT(mc) FROM MilkCollection mc WHERE mc.farmerId = :farmerId GROUP BY mc.status")
    List<Object[]> countByFarmerIdGroupByStatus(@Param("farmerId") Long farmerId);

    // Collectes d'un agriculteur triées par date descendante
    @Query("SELECT mc FROM MilkCollection mc WHERE mc.farmerId = :farmerId ORDER BY mc.collectedAt DESC")
    List<MilkCollection> findLatestByFarmerId(@Param("farmerId") Long farmerId);
}
