package org.milkcenter.fleetservice.repository;

import org.milkcenter.fleetservice.enums.DriverStatus;
import org.milkcenter.fleetservice.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface DriverRepository extends JpaRepository <Driver, Long> {

    Optional<Driver> findByUserId(Long userId);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    boolean existsByUserId(Long userId);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long driverId );

    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findByStatus(DriverStatus status);


    boolean existsByUserIdAndIdNot(
            Long userId,
            Long driverId
    );



    List<Driver> findAllByOrderBySalaryDesc();



}
