package org.milkcenter.fleetservice.repository;

import org.milkcenter.fleetservice.enums.VehicleStatus;
import org.milkcenter.fleetservice.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository <Vehicle , Long> {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsById(Long id);

    boolean existsByLicensePlate(String licensePlate);


    List<Vehicle> findByStatus(VehicleStatus status);


    List<Vehicle> findByModel(String model);


    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);


}
