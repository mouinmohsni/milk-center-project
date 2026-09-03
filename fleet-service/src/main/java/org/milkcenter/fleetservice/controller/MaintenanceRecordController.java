package org.milkcenter.fleetservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.maintenance.MaintenanceRecordRequest;
import org.milkcenter.fleetservice.dto.request.maintenance.MaintenanceRecordUpdateRequest;
import org.milkcenter.fleetservice.dto.response.MaintenanceRecordResponse;
import org.milkcenter.fleetservice.service.MaintenanceRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenances" )
@RequiredArgsConstructor
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    /**
     * Crée une nouvelle maintenance (Statut IN_PROGRESS par défaut).
     * Le véhicule passera automatiquement en statut MAINTENANCE.
     */
    @PostMapping
    public ResponseEntity<MaintenanceRecordResponse> createMaintenance(
            @Valid @RequestBody MaintenanceRecordRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceRecordService.createMaintenance(request));
    }

    /**
     * Met à jour ou termine une maintenance.
     * Si le statut passe à COMPLETED, le véhicule repasse en AVAILABLE.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<MaintenanceRecordResponse> updateMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRecordUpdateRequest request
    ) {
        return ResponseEntity.ok(maintenanceRecordService.updateMaintenance(id, request));
    }

    /**
     * Récupère l'historique des maintenances d'un véhicule.
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceRecordResponse>> getByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(maintenanceRecordService.getMaintenanceByVehicle(vehicleId));
    }

    /**
     * Récupère les détails d'une maintenance précise.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRecordResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceRecordService.getMaintenanceById(id));
    }

    /**
     * Supprime un enregistrement de maintenance.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenance(@PathVariable Long id) {
        maintenanceRecordService.deleteMaintenance(id);
        return ResponseEntity.noContent().build();
    }
}
