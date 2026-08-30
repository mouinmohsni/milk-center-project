package org.milkcenter.fleetservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.vehicle.*;
import org.milkcenter.fleetservice.dto.response.VehicleResponse;
import org.milkcenter.fleetservice.enums.VehicleStatus;
import org.milkcenter.fleetservice.service.VehiculeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/vehicles" )
@RequiredArgsConstructor
public class VehicleController {

    private final VehiculeService vehiculeService;

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehiculeService.getAllVehiculs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(vehiculeService.getVehiculById(id));
    }

    @GetMapping("/license/{licensePlate}")
    public ResponseEntity<VehicleResponse> getVehicleByLicensePlate(
            @PathVariable String licensePlate
    ) {
        return ResponseEntity.ok(
                vehiculeService.getVehiculBylicensePlate(licensePlate)
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByStatus(
            @PathVariable VehicleStatus status
    ) {
        return ResponseEntity.ok(
                vehiculeService.getVehiclesByStatus(status)
        );
    }

    @GetMapping("/model/{model}")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByModel(
            @PathVariable String model
    ) {
        return ResponseEntity.ok(
                vehiculeService.getVehiclesByModel(model)
        );
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehiculeService.createVehicul(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicleByManager(
            @PathVariable Long id,
            @Valid @RequestBody VehicleManagerUpdateRequest request
    ) {
        return ResponseEntity.ok(
                vehiculeService.updateVehicleByManager(id, request)
        );
    }

    @PatchMapping("/{id}/operations")
    public ResponseEntity<VehicleResponse> updateVehicleByOperator(
            @PathVariable Long id,
            @Valid @RequestBody VehicleOperationsUpdateRequest request
    ) {
        return ResponseEntity.ok(
                vehiculeService.updateVehicleByOperator(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VehicleResponse> updateVehicleStatus(
            @PathVariable Long id,
            @Valid @RequestBody VehicleStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                vehiculeService.updateVehicleStatus(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long id
    ) {
        vehiculeService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
