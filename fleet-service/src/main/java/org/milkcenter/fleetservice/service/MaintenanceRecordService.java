package org.milkcenter.fleetservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.maintenance.MaintenanceRecordRequest;
import org.milkcenter.fleetservice.dto.request.maintenance.MaintenanceRecordUpdateRequest;
import org.milkcenter.fleetservice.dto.response.MaintenanceRecordResponse;
import org.milkcenter.fleetservice.enums.MaintenanceStatus;
import org.milkcenter.fleetservice.enums.MaintenanceType;
import org.milkcenter.fleetservice.enums.VehicleStatus;
import org.milkcenter.fleetservice.model.MaintenanceRecord;
import org.milkcenter.fleetservice.model.Vehicle;
import org.milkcenter.fleetservice.repository.MaintenanceRecordRepository;
import org.milkcenter.fleetservice.repository.VehicleRepository;
import org.milkcenter.fleetservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;

    /**
     * Seul le MANAGER peut gérer la maintenance.
     */
    private void checkManagerAccess( ) {
        if (!"ROLE_MANAGER".equals(currentUserService.getCurrentRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux gestionnaires");
        }
    }

    /**
     * Enregistre l'entrée en maintenance d'un véhicule.
     * Statut initial : IN_PROGRESS. Statut véhicule : MAINTENANCE.
     */
    @Transactional
    public MaintenanceRecordResponse createMaintenance(MaintenanceRecordRequest request) {
        checkManagerAccess();

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule non trouvé"));

        MaintenanceRecord record = MaintenanceRecord.builder()
                .vehicle(vehicle)
                .maintenanceType(request.getMaintenanceType())
                .description(request.getDescription())
                .maintenanceDate(request.getMaintenanceDate())
                .status(MaintenanceStatus.IN_PROGRESS)
                .build();

        // Le véhicule est maintenant indisponible car il est au garage
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.save(vehicle);

        return mapToResponse(maintenanceRecordRepository.save(record));
    }

    /**
     * Clôture une maintenance avec les détails de la facture et du compteur.
     * Statut final : COMPLETED. Statut véhicule : READY.
     */
    @Transactional
    public MaintenanceRecordResponse completeMaintenance(Long id, MaintenanceRecordUpdateRequest request) {
        checkManagerAccess();

        MaintenanceRecord record = maintenanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance non trouvée"));

        if (record.getStatus() == MaintenanceStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette maintenance est déjà terminée");
        }

        // Validation des données de clôture
        if (request.getOdometer() == null || request.getCost() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le kilométrage (odometer) et le coût sont obligatoires pour clôturer la maintenance");
        }

        record.setOdometer(request.getOdometer());
        record.setCost(request.getCost());
        record.setProvider(request.getProvider());
        record.setNextMaintenanceOdometer(request.getNextMaintenanceOdometer());
        record.setStatus(MaintenanceStatus.COMPLETED);

        // Mise à jour du véhicule : il redevient opérationnel
        Vehicle vehicle = record.getVehicle();
        vehicle.setStatus(VehicleStatus.READY);

        // Mise à jour des compteurs si nécessaire
        if (request.getOdometer() > vehicle.getKm()) {
            vehicle.setKm(request.getOdometer());
        }

        // Si c'est une vidange, on met à jour le kilométrage de référence
        if (record.getMaintenanceType() == MaintenanceType.OIL_CHANGE) {
            vehicle.setLastOilChangeMileage(request.getOdometer());
        }

        vehicleRepository.save(vehicle);
        return mapToResponse(maintenanceRecordRepository.save(record));
    }

    /**
     * Mise à jour générique (changement de type, description, ou annulation).
     */
    @Transactional
    public MaintenanceRecordResponse updateMaintenance(Long id, MaintenanceRecordUpdateRequest request) {
        checkManagerAccess();

        MaintenanceRecord record = maintenanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance non trouvée"));

        // Gestion du changement de statut via update
        if (request.getStatus() != null) {
            if (request.getStatus() == MaintenanceStatus.COMPLETED) {
                return completeMaintenance(id, request);
            }
            if (request.getStatus() == MaintenanceStatus.CANCELLED && record.getStatus() == MaintenanceStatus.IN_PROGRESS) {
                record.setStatus(MaintenanceStatus.CANCELLED);
                Vehicle v = record.getVehicle();
                v.setStatus(VehicleStatus.READY);
                vehicleRepository.save(v);
            }
        }

        // Mises à jour des champs informatifs
        if (request.getMaintenanceType() != null) record.setMaintenanceType(request.getMaintenanceType());
        if (request.getDescription() != null) record.setDescription(request.getDescription());
        if (request.getMaintenanceDate() != null) record.setMaintenanceDate(request.getMaintenanceDate());
        if (request.getProvider() != null) record.setProvider(request.getProvider());

        return mapToResponse(maintenanceRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecordResponse> getMaintenanceByVehicle(Long vehicleId) {
        // Le Manager peut tout voir, le Driver peut voir l'historique de son véhicule affecté (optionnel)
        return maintenanceRecordRepository.findByVehicle_IdOrderByMaintenanceDateDesc(vehicleId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaintenanceRecordResponse getMaintenanceById(Long id) {
        return maintenanceRecordRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance non trouvée"));
    }

    @Transactional
    public void deleteMaintenance(Long id) {
        checkManagerAccess();
        maintenanceRecordRepository.deleteById(id);
    }

    private MaintenanceRecordResponse mapToResponse(MaintenanceRecord record) {
        return MaintenanceRecordResponse.builder()
                .id(record.getId())
                .vehicleId(record.getVehicle().getId())
                .licensePlate(record.getVehicle().getLicensePlate())
                .status(record.getStatus())
                .maintenanceType(record.getMaintenanceType())
                .description(record.getDescription())
                .maintenanceDate(record.getMaintenanceDate())
                .odometer(record.getOdometer())
                .cost(record.getCost())
                .provider(record.getProvider())
                .nextMaintenanceOdometer(record.getNextMaintenanceOdometer())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
