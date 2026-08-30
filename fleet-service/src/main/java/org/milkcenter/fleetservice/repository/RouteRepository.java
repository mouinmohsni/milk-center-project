package org.milkcenter.fleetservice.repository;

import org.milkcenter.fleetservice.enums.RouteStatus;
import org.milkcenter.fleetservice.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route,Long> {
    List<Route> findByDriverId(Long driverId);

    List<Route> findByVehicleId(Long vehicleId);

    List<Route> findByStatus(RouteStatus status);

}
