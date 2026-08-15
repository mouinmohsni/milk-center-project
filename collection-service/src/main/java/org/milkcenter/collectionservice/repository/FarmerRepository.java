package org.milkcenter.collectionservice.repository;

import org.milkcenter.collectionservice.model.FarmerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface FarmerRepository extends JpaRepository<FarmerProfile,Long> {

    Optional<FarmerProfile> findByUserId(Long userId);

    Optional<FarmerProfile> findByFarmName(String farmName);

    Optional<FarmerProfile> findByAddress(String address);

    boolean existsByUserId(Long userId);


    Collection<FarmerProfile> findByActiveTrue();
}
