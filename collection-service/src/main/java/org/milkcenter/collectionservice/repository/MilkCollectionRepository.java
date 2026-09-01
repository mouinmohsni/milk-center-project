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

    List<MilkCollection> findByFarmerId(Long farmerId);

    List<MilkCollection> findByDriverUserId(Long driverUserId);

    List<MilkCollection> findByRouteStopId(Long routeStopId);

    List<MilkCollection> findByFarmerIdAndStatus(Long farmerId, CollectionStatus status);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<MilkCollection> findByIdempotencyKey(String idempotencyKey);

    List<MilkCollection> findByCollectedAtBetween(Date start, Date end);

    @Query("SELECT SUM(mc.quantityLiters) FROM MilkCollection mc WHERE mc.farmerId = :farmerId AND mc.status = :status AND mc.collectedAt BETWEEN :start AND :end")
    BigDecimal sumQuantityByFarmerIdAndStatusAndPeriod(
            @Param("farmerId") Long farmerId,
            @Param("status") CollectionStatus status,
            @Param("start") Date start,
            @Param("end") Date end
    );

    @Query("SELECT SUM(mc.quantityLiters) FROM MilkCollection mc WHERE mc.farmerId = :farmerId AND mc.status = 'ACCEPTED'")
    BigDecimal sumAcceptedQuantityByFarmerId(@Param("farmerId") Long farmerId);

    @Query("SELECT mc.status, COUNT(mc) FROM MilkCollection mc WHERE mc.farmerId = :farmerId GROUP BY mc.status")
    List<Object[]> countByFarmerIdGroupByStatus(@Param("farmerId") Long farmerId);

    @Query("SELECT mc FROM MilkCollection mc WHERE mc.farmerId = :farmerId ORDER BY mc.collectedAt DESC")
    List<MilkCollection> findLatestByFarmerId(@Param("farmerId") Long farmerId);
}
