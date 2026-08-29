package org.milkcenter.fleetservice.service;


import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.response.DriverResponse;
import org.milkcenter.fleetservice.enums.DriverStatus;
import org.milkcenter.fleetservice.model.Driver;
import org.milkcenter.fleetservice.repository.DriverRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverService {

     private final DriverRepository driverRepository ;


     public List<DriverResponse> getAllDrivers(){
         return driverRepository.findAll()
                 .stream()
                 .map(this::mapToResponse)
                 .collect(Collectors.toList());
     }

    public List<DriverResponse> getDriversByStatus(String status){
        return driverRepository.findDriverByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DriverResponse getDriversById(Long id){
         Driver driver = driverRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "le driver non trouvé avec  ID: " + id
        ));
         return mapToResponse(driver);
    }

    public DriverResponse getDriversByUserId(Long id){
        Driver driver = driverRepository.findByUserId(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "le driver non trouvé avec  le user id: " + id
        ));
        return mapToResponse(driver);
    }


        // ============================================
    // Mapper privé — Entité → DTO Response
    // ============================================
    private DriverResponse mapToResponse(Driver driver ){
        return DriverResponse.builder()
                .id(driver.getId())
                .userId(driver.getUserId())
                .licenseNumber(driver.getLicenseNumber())
                .salary(driver.getSalary())
                .status(driver.getStatus())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
