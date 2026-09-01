package org.milkcenter.collectionservice.repository;

import org.milkcenter.collectionservice.model.FarmerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FarmerRepository extends JpaRepository<FarmerProfile, Long> {

    Optional<FarmerProfile> findByUserId(Long userId);

    Optional<FarmerProfile> findByFarmName(String farmName);

    List<FarmerProfile> findByAddress(String address);

    boolean existsByUserId(Long userId);

    List<FarmerProfile> findByActiveTrue();
}
