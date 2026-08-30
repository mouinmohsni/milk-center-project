package org.milkcenter.fleetservice.repository;

import org.milkcenter.fleetservice.enums.AssignmentStatusRouteStop;
import org.milkcenter.fleetservice.model.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteStopRepository extends JpaRepository <RouteStop , Long> {

    List<RouteStop> findByRoute_Id(Long routeId);

    List<RouteStop> findByFarmerId(Long farmerId);

    List<RouteStop> findByAssignmentStatus(
            AssignmentStatusRouteStop assignmentStatus
    );
}
