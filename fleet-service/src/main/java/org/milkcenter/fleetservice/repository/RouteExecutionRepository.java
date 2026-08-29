package org.milkcenter.fleetservice.repository;
import org.milkcenter.fleetservice.model.RouteExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;



public interface RouteExecutionRepository
        extends JpaRepository<RouteExecution, Long> {

    Optional<RouteExecution> findByRoute_IdAndExecutionDate(
            Long routeId,
            Date executionDate
    );

    boolean existsByRoute_IdAndExecutionDate(
            Long routeId,
            Date executionDate
    );

    List<RouteExecution> findByRoute_IdOrderByExecutionDateDesc(
            Long routeId
    );

    List<RouteExecution> findByActualDriver_IdOrderByExecutionDateDesc(
            Long driverId
    );

    List<RouteExecution> findByActualVehicle_IdOrderByExecutionDateDesc(
            Long vehicleId
    );
}

