package org.milkcenter.fleetservice.repository;

import org.milkcenter.fleetservice.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface DriverRepository extends JpaRepository <Driver, Long> {

    Optional<Driver> findByUserId(Long userId);

    @Query("SELECT dr FROM Driver  dr WHERE dr.status = :status")
    List<Driver> findDriverByStatus (
            @Param("status") String status
    );
}
