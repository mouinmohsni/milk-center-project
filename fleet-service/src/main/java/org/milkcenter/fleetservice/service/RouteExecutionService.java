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
import org.milkcenter.fleetservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteExecutionService {

    private final RouteExecutionRepository routeExecutionRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;

    // =====================================================
    // RECHERCHE
    // =====================================================

    /**
     * Retourne toutes les exécutions.
     * Cette recherche globale est réservée au MANAGER.
     */
    public List<RouteExecutionResponse> getAllExecutions( ) {
        requireManager();

        return routeExecutionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recherche une exécution par son identifiant.
     * Un DRIVER ne peut consulter que sa propre exécution.
     */
    public RouteExecutionResponse getExecutionById(Long id) {
        RouteExecution execution = findExecutionById(id);
        checkExecutionAccess(execution);

        return mapToResponse(execution);
    }

    /**
     * Retourne l'historique des exécutions d'une route.
     * Cette recherche globale reste réservée au MANAGER.
     */
    public List<RouteExecutionResponse> getExecutionsByRouteId(Long routeId) {
        requireManager();
        findRouteById(routeId);

        return routeExecutionRepository
                .findByRoute_IdOrderByExecutionDateDesc(routeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retourne l'historique d'un chauffeur.
     * Le DRIVER ne peut consulter que son propre historique.
     */
    public List<RouteExecutionResponse> getExecutionsByDriverId(Long driverId) {
        Driver driver = findDriverById(driverId);
        String role = currentUserService.getCurrentRole();

        if ("DRIVER".equals(role)) {
            Long currentUserId = currentUserService.getCurrentUserId();

            if (!Objects.equals(driver.getUserId(), currentUserId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Vous ne pouvez consulter que vos propres exécutions"
                );
            }
        } else if (!"MANAGER".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Rôle non autorisé pour cette opération"
            );
        }

        return routeExecutionRepository
                .findByActualDriver_IdOrderByExecutionDateDesc(driverId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retourne l'historique d'un véhicule.
     * Cette recherche est réservée au MANAGER.
     */
    public List<RouteExecutionResponse> getExecutionsByVehicleId(Long vehicleId) {
        requireManager();
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
     * Cette opération est réservée au MANAGER.
     */
    public RouteExecutionResponse createExecution(
            RouteExecutionRequest request
    ) {
        requireManager();

        Route route = findRouteById(request.getRouteId());

        if (route.getStatus() == RouteStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Impossible de créer une exécution pour une route annulée"
            );
        }

        if (routeExecutionRepository.existsByRoute_IdAndExecutionDate(
                request.getRouteId(),
                request.getExecutionDate()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une exécution existe déjà pour cette route et cette date"
            );
        }

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
     * Cette opération reste réservée au MANAGER.
     */
    public RouteExecutionResponse updateExecution(
            Long id,
            RouteExecutionUpdateRequest request
    ) {
        requireManager();

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
     * Change le statut d'une exécution.
     * Le MANAGER peut modifier toute exécution.
     * Le DRIVER peut modifier uniquement son exécution affectée.
     */
    public RouteExecutionResponse updateExecutionStatus(
            Long id,
            RouteExecutionStatusUpdateRequest request
    ) {
        RouteExecution execution = findExecutionById(id);
        checkExecutionAccess(execution);

        String role = currentUserService.getCurrentRole();
        RouteExecutionStatus newStatus = request.getStatus();

        // Le DRIVER ne modifie pas l'annulation d'une exécution.
        // Cette décision administrative reste réservée au MANAGER.
        if ("DRIVER".equals(role)
                && newStatus == RouteExecutionStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Le DRIVER ne peut pas annuler une exécution"
            );
        }

        validateStatusTransition(
                execution.getStatus(),
                newStatus
        );

        execution.setStatus(newStatus);

        if (newStatus == RouteExecutionStatus.ACTIVE
                && execution.getStartedAt() == null) {
            execution.setStartedAt(new Date());
        }

        if (newStatus == RouteExecutionStatus.FINISHED
                && execution.getFinishedAt() == null) {
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
     * Vérifie les transitions autorisées entre les statuts.
     */
    private void validateStatusTransition(
            RouteExecutionStatus currentStatus,
            RouteExecutionStatus newStatus
    ) {
        if (newStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nouveau statut est obligatoire"
            );
        }

        if (currentStatus == newStatus) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'exécution possède déjà ce statut"
            );
        }

        if (currentStatus == RouteExecutionStatus.FINISHED
                || currentStatus == RouteExecutionStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une exécution terminée ou annulée ne peut plus être modifiée"
            );
        }

        boolean validTransition =
                (currentStatus == RouteExecutionStatus.PLANNED
                        && (newStatus == RouteExecutionStatus.ACTIVE
                        || newStatus == RouteExecutionStatus.FINISHED
                        || newStatus == RouteExecutionStatus.CANCELLED))
                        || (currentStatus == RouteExecutionStatus.ACTIVE
                        && (newStatus == RouteExecutionStatus.FINISHED
                        || newStatus == RouteExecutionStatus.CANCELLED));

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transition de statut non autorisée : "
                            + currentStatus + " vers " + newStatus
            );
        }
    }

    // =====================================================
    // SUPPRESSION
    // =====================================================

    /**
     * Suppression physique exceptionnelle, réservée au MANAGER.
     */
    public void deleteExecution(Long id) {
        requireManager();

        RouteExecution execution = findExecutionById(id);
        routeExecutionRepository.delete(execution);
    }

    // =====================================================
    // CONTROLES D'ACCES
    // =====================================================

    /**
     * Vérifie l'accès à une exécution précise.
     */
    private void checkExecutionAccess(RouteExecution execution) {
        String role = currentUserService.getCurrentRole();

        if ("MANAGER".equals(role)) {
            return;
        }

        if ("DRIVER".equals(role)) {
            Long currentUserId = currentUserService.getCurrentUserId();

            if (execution.getActualDriver() == null
                    || !Objects.equals(
                    execution.getActualDriver().getUserId(),
                    currentUserId
            )) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Cette exécution ne vous est pas affectée"
                );
            }

            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Rôle non autorisé pour cette opération"
        );
    }

    /**
     * Vérifie que l'utilisateur connecté est MANAGER.
     */
    private void requireManager() {
        if (!"MANAGER".equals(currentUserService.getCurrentRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cette opération est réservée au MANAGER"
            );
        }
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

    /** Transforme l'entité en DTO de réponse. */
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
