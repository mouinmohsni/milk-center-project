package org.milkcenter.fleetservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.fuel.FuelConsumptionRequest;
import org.milkcenter.fleetservice.dto.request.fuel.FuelConsumptionUpdateRequest;
import org.milkcenter.fleetservice.dto.response.FuelConsumptionResponse;
import org.milkcenter.fleetservice.service.FuelConsumptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fuel-consumptions" )
@RequiredArgsConstructor
public class FuelConsumptionController {

    private final FuelConsumptionService fuelConsumptionService;

    /**
     * Enregistre un achat de carburant.
     * Met à jour le kilométrage du véhicule si nécessaire.
     */
    @PostMapping
    public ResponseEntity<FuelConsumptionResponse> createFuel(
            @Valid @RequestBody FuelConsumptionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fuelConsumptionService.createFuelConsumption(request));
    }

    /**
     * Modifie un enregistrement de carburant (ex: corriger le prix ou les tournées liées).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<FuelConsumptionResponse> updateFuel(
            @PathVariable Long id,
            @Valid @RequestBody FuelConsumptionUpdateRequest request
    ) {
        return ResponseEntity.ok(fuelConsumptionService.updateFuelConsumption(id, request));
    }

    /**
     * Liste les pleins de carburant pour un véhicule.
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<FuelConsumptionResponse>> getByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(fuelConsumptionService.getFuelByVehicle(vehicleId));
    }

    /**
     * Supprime un enregistrement de carburant.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFuel(@PathVariable Long id) {
        fuelConsumptionService.deleteFuel(id);
        return ResponseEntity.noContent().build();
    }
}
