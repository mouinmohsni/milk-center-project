package org.milkcenter.fleetservice.repository;

import org.milkcenter.fleetservice.model.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteStopRepository extends JpaRepository <RouteStop , Long> {
}
