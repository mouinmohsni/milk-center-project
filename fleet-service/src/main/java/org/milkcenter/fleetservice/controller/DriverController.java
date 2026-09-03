package org.milkcenter.fleetservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.driver.*;
import org.milkcenter.fleetservice.dto.response.DriverResponse;
import org.milkcenter.fleetservice.enums.DriverStatus;
import org.milkcenter.fleetservice.security.CurrentUserService;
import org.milkcenter.fleetservice.service.DriverService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/drivers" )
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final CurrentUserService currentUserService;

    // Retourner tous les chauffeurs.
    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    // Retourner un chauffeur par son ID.
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    // Rechercher un chauffeur grâce au userId provenant de identity-service.
    @GetMapping("/user/{userId}")
    public ResponseEntity<DriverResponse> getDriverByUserId(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                driverService.getDriverByUserId(userId)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<DriverResponse> getDriverByUserId() {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(
                driverService.getDriverByUserId(userId)
        );
    }



    // Rechercher un chauffeur par son numéro de permis.
    @GetMapping("/license/{licenseNumber}")
    public ResponseEntity<DriverResponse> getDriverByLicenseNumber(
            @PathVariable String licenseNumber
    ) {
        return ResponseEntity.ok(
                driverService.getDriverByLicenseNumber(licenseNumber)
        );
    }

    // Retourner les chauffeurs selon leur statut.
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DriverResponse>> getDriversByStatus(
            @PathVariable DriverStatus status
    ) {
        return ResponseEntity.ok(
                driverService.getDriversByStatus(status)
        );
    }

    // Retourner uniquement les chauffeurs disponibles.
    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers() {
        return ResponseEntity.ok(
                driverService.getAvailableDrivers()
        );
    }

    // Retourner les chauffeurs classés par salaire décroissant.
    @GetMapping("/salary")
    public ResponseEntity<List<DriverResponse>> getDriversBySalary() {
        return ResponseEntity.ok(
                driverService.getDriversBySalary()
        );
    }

    // Créer un nouveau chauffeur.
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody DriverRequest request
    ) {
        DriverResponse response = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Modifier les informations administratives du chauffeur.
    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverUpdateRequest request
    ) {
        return ResponseEntity.ok(
                driverService.updateDriver(id, request)
        );
    }

    // Modifier uniquement le statut du chauffeur.
    @PatchMapping("/{id}/status")
    public ResponseEntity<DriverResponse> updateStatusDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                driverService.updateStatusDriver(id, request)
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(
            @PathVariable Long id
    ) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }


}
