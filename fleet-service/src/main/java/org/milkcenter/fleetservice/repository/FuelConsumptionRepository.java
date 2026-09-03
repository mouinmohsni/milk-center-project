package org.milkcenter.fleetservice.repository;

import org.milkcenter.fleetservice.model.FuelConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuelConsumptionRepository extends JpaRepository<FuelConsumption, Long> {
    List<FuelConsumption> findByVehicleIdOrderByFuelDateDesc(Long vehicleId);
}
