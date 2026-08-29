package org.milkcenter.fleetservice.repository;

import org.milkcenter.fleetservice.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository <Vehicle , Long> {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

}
