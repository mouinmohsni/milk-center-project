package org.milkcenter.fleetservice.repository;

import org.milkcenter.fleetservice.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByVehicle_IdOrderByMaintenanceDateDesc(Long vehicleId);

}
