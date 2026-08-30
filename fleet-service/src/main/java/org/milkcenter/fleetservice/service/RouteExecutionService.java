package org.milkcenter.fleetservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.routeExecution.RouteExecutionRequest;
import org.milkcenter.fleetservice.dto.request.routeExecution.RouteExecutionStatusUpdateRequest;
import org.milkcenter.fleetservice.dto.request.routeExecution.RouteExecutionUpdateRequest;
import org.milkcenter.fleetservice.dto.response.RouteExecutionResponse;
import org.milkcenter.fleetservice.enums.DriverStatus;
import org.milkcenter.fleetservice.enums.RouteExecutionStatus;
import org.milkcenter.fleetservice.enums.RouteStatus;
import org.milkcenter.fleetservice.enums.VehicleStatus;
import org.milkcenter.fleetservice.model.Driver;
import org.milkcenter.fleetservice.model.Route;
import org.milkcenter.fleetservice.model.RouteExecution;
import org.milkcenter.fleetservice.model.Vehicle;
import org.milkcenter.fleetservice.repository.DriverRepository;
import org.milkcenter.fleetservice.repository.RouteExecutionRepository;
import org.milkcenter.fleetservice.repository.RouteRepository;
import org.milkcenter.fleetservice.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteExecutionService {

    private final RouteExecutionRepository routeExecutionRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    // =====================================================
    // RECHERCHE
    // =====================================================

    /** Retourne toutes les exécutions. */
    public List<RouteExecutionResponse> getAllExecutions( ) {
        return routeExecutionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Recherche une exécution par son identifiant. */
    public RouteExecutionResponse getExecutionById(Long id) {
        return mapToResponse(findExecutionById(id));
    }

    /** Retourne l'historique des exécutions d'une route. */
    public List<RouteExecutionResponse> getExecutionsByRouteId(Long routeId) {
        findRouteById(routeId);

        return routeExecutionRepository
                .findByRoute_IdOrderByExecutionDateDesc(routeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Retourne l'historique d'un chauffeur. */
    public List<RouteExecutionResponse> getExecutionsByDriverId(
            Long driverId
    ) {
        findDriverById(driverId);

        return routeExecutionRepository
                .findByActualDriver_IdOrderByExecutionDateDesc(driverId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Retourne l'historique d'un véhicule. */
    public List<RouteExecutionResponse> getExecutionsByVehicleId(
            Long vehicleId
    ) {
        findVehicleById(vehicleId);

        return routeExecutionRepository
                .findByActualVehicle_IdOrderByExecutionDateDesc(vehicleId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // CREATION
    // =====================================================

    /**
     * Crée une exécution pour une date donnée.
     *
     * Si le chauffeur ou le véhicule réel n'est pas fourni,
     * on utilise l'affectation habituelle de la route.
     */
    public RouteExecutionResponse createExecution(
            RouteExecutionRequest request
    ) {
        Route route = findRouteById(request.getRouteId());

        // Une route annulée ne peut pas avoir de nouvelle exécution.
        if (route.getStatus() == RouteStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Impossible de créer une exécution pour une route annulée"
            );
        }

        // Une seule exécution est autorisée pour une route et une date.
        if (routeExecutionRepository.existsByRoute_IdAndExecutionDate(
                request.getRouteId(),
                request.getExecutionDate()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une exécution existe déjà pour cette route et cette date"
            );
        }

        // Si aucun remplacement n'est fourni, on utilise les affectations
        // habituelles enregistrées dans Route.
        Driver actualDriver = request.getActualDriverId() != null
                ? findDriverById(request.getActualDriverId())
                : route.getDriver();

        Vehicle actualVehicle = request.getActualVehicleId() != null
                ? findVehicleById(request.getActualVehicleId())
                : route.getVehicle();

        if (actualDriver == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucun chauffeur n'est affecté à cette exécution"
            );
        }

        if (actualVehicle == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucun véhicule n'est affecté à cette exécution"
            );
        }

        // On vérifie la disponibilité uniquement si l'exécution
        // est créée avec l'affectation actuelle.
        validateDriverAvailability(actualDriver);
        validateVehicleAvailability(actualVehicle);

        RouteExecution execution = RouteExecution.builder()
                .route(route)
                .actualDriver(actualDriver)
                .actualVehicle(actualVehicle)
                .executionDate(request.getExecutionDate())
                .status(RouteExecutionStatus.PLANNED)
                .build();

        RouteExecution savedExecution =
                routeExecutionRepository.save(execution);

        return mapToResponse(savedExecution);
    }

    // =====================================================
    // MODIFICATION PARTIELLE
    // =====================================================

    /**
     * Modifie la date, le chauffeur réel ou le véhicule réel.
     * Le statut est modifié par updateExecutionStatus().
     */
    public RouteExecutionResponse updateExecution(
            Long id,
            RouteExecutionUpdateRequest request
    ) {
        RouteExecution execution = findExecutionById(id);

        if (request.getExecutionDate() == null
                && request.getActualDriverId() == null
                && request.getActualVehicleId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Au moins un champ doit être fourni"
            );
        }

        if (request.getExecutionDate() != null
                && !request.getExecutionDate()
                .equals(execution.getExecutionDate())) {

            if (routeExecutionRepository
                    .existsByRoute_IdAndExecutionDateAndIdNot(
                            execution.getRoute().getId(),
                            request.getExecutionDate(),
                            id
                    )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Une exécution existe déjà pour cette route et cette date"
                );
            }

            execution.setExecutionDate(request.getExecutionDate());
        }

        if (request.getActualDriverId() != null) {
            Driver driver = findDriverById(request.getActualDriverId());
            validateDriverAvailability(driver);
            execution.setActualDriver(driver);
        }

        if (request.getActualVehicleId() != null) {
            Vehicle vehicle = findVehicleById(request.getActualVehicleId());
            validateVehicleAvailability(vehicle);
            execution.setActualVehicle(vehicle);
        }

        RouteExecution updatedExecution =
                routeExecutionRepository.save(execution);

        return mapToResponse(updatedExecution);
    }

    // =====================================================
    // MODIFICATION DU STATUT
    // =====================================================

    /**
     * Change le statut et gère automatiquement les dates de début
     * et de fin de l'exécution.
     */
    public RouteExecutionResponse updateExecutionStatus(
            Long id,
            RouteExecutionStatusUpdateRequest request
    ) {
        RouteExecution execution = findExecutionById(id);
        RouteExecutionStatus newStatus = request.getStatus();

        validateStatusTransition(execution.getStatus(), newStatus);

        execution.setStatus(newStatus);

        if (newStatus == RouteExecutionStatus.ACTIVE
                && execution.getStartedAt() == null) {
            execution.setStartedAt(new Date());
        }

        if (newStatus == RouteExecutionStatus.FINISHED
                && execution.getFinishedAt() == null) {
            // Si l'exécution passe directement de PLANNED à FINISHED,
            // on renseigne également sa date de début.
            if (execution.getStartedAt() == null) {
                execution.setStartedAt(new Date());
            }
            execution.setFinishedAt(new Date());
        }

        RouteExecution updatedExecution =
                routeExecutionRepository.save(execution);

        return mapToResponse(updatedExecution);
    }

    // =====================================================
    // VALIDATIONS METIER
    // =====================================================

    /** Vérifie qu'un chauffeur existe et est disponible. */
    private void validateDriverAvailability(Driver driver) {
        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le chauffeur n'est pas disponible. Statut actuel : "
                            + driver.getStatus()
            );
        }
    }

    /** Vérifie qu'un véhicule existe et est prêt. */
    private void validateVehicleAvailability(Vehicle vehicle) {
        if (vehicle.getStatus() != VehicleStatus.READY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le véhicule n'est pas disponible. Statut actuel : "
                            + vehicle.getStatus()
            );
        }
    }

    /**
     * Empêche les changements incohérents de statut.
     */
    private void validateStatusTransition(
            RouteExecutionStatus currentStatus,
            RouteExecutionStatus newStatus
    ) {
        if (currentStatus == RouteExecutionStatus.FINISHED
                || currentStatus == RouteExecutionStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une exécution terminée ou annulée ne peut plus être modifiée"
            );
        }

        if (currentStatus == RouteExecutionStatus.PLANNED
                && newStatus == RouteExecutionStatus.FINISHED) {
            // Ce changement reste permis : le Service renseigne
            // automatiquement startedAt et finishedAt.
            return;
        }
    }

    // =====================================================
    // SUPPRESSION
    // =====================================================

    /**
     * Suppression physique exceptionnelle.
     * Il est préférable de passer le statut à CANCELLED pour préserver
     * l'historique d'une exécution déjà créée.
     */
    public void deleteExecution(Long id) {
        RouteExecution execution = findExecutionById(id);
        routeExecutionRepository.delete(execution);
    }

    // =====================================================
    // METHODES PRIVEES
    // =====================================================

    private RouteExecution findExecutionById(Long id) {
        return routeExecutionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucune exécution trouvée avec l'ID : " + id
                ));
    }

    private Route findRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucune route trouvée avec l'ID : " + id
                ));
    }

    private Driver findDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun chauffeur trouvé avec l'ID : " + id
                ));
    }

    private Vehicle findVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun véhicule trouvé avec l'ID : " + id
                ));
    }

    /** Transforme une entité en DTO de réponse sans exposer les entités liées. */
    private RouteExecutionResponse mapToResponse(
            RouteExecution execution
    ) {
        return RouteExecutionResponse.builder()
                .id(execution.getId())
                .routeId(
                        execution.getRoute() != null
                                ? execution.getRoute().getId()
                                : null
                )
                .actualDriverId(
                        execution.getActualDriver() != null
                                ? execution.getActualDriver().getId()
                                : null
                )
                .actualVehicleId(
                        execution.getActualVehicle() != null
                                ? execution.getActualVehicle().getId()
                                : null
                )
                .executionDate(execution.getExecutionDate())
                .status(execution.getStatus())
                .startedAt(execution.getStartedAt())
                .finishedAt(execution.getFinishedAt())
                .createdAt(execution.getCreatedAt())
                .updatedAt(execution.getUpdatedAt())
                .build();
    }
}
