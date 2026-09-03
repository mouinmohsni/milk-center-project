package org.milkcenter.fleetservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.routeStop.RouteStopAssignmentRequest;
import org.milkcenter.fleetservice.dto.request.routeStop.RouteStopRequest;
import org.milkcenter.fleetservice.dto.request.routeStop.RouteStopUpdateRequest;
import org.milkcenter.fleetservice.dto.response.RouteStopResponse;
import org.milkcenter.fleetservice.enums.AssignmentStatusRouteStop;
import org.milkcenter.fleetservice.enums.RouteExecutionStatus;
import org.milkcenter.fleetservice.model.Route;
import org.milkcenter.fleetservice.model.RouteExecution;
import org.milkcenter.fleetservice.model.RouteStop;
import org.milkcenter.fleetservice.repository.RouteExecutionRepository;
import org.milkcenter.fleetservice.repository.RouteRepository;
import org.milkcenter.fleetservice.repository.RouteStopRepository;
import org.milkcenter.fleetservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteStopService {

    private final RouteStopRepository routeStopRepository;
    private final RouteRepository routeRepository;
    private final CurrentUserService currentUserService;
    private final RouteExecutionRepository routeExecutionRepository;

    // =====================================================
    // RECHERCHE
    // =====================================================

    /**
     * Retourne tous les arrêts.
     * Cette recherche globale est réservée au MANAGER.
     */
    public List<RouteStopResponse> getAllStops( ) {
        requireManager();

        return routeStopRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recherche un arrêt par son identifiant.
     * Le DRIVER et le FARMER sont contrôlés selon leur propriété.
     */
    public RouteStopResponse getStopById(Long id) {
        RouteStop routeStop = findStopById(id);

        Long routeId = routeStop.getRoute() != null
                ? routeStop.getRoute().getId()
                : null;

        checkAccess(routeStop.getId(), routeId);

        return mapToResponse(routeStop);
    }

    /**
     * Retourne les arrêts d'une route donnée.
     */
    public List<RouteStopResponse> getStopsByRouteId(Long routeId) {
        // Vérifie que la route existe et que l'utilisateur peut la consulter.
        findRouteById(routeId);
        checkRouteAccess(routeId);

        return routeStopRepository.findByRoute_Id(routeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les arrêts d'un fermier.
     */
    public List<RouteStopResponse> getStopsByFarmerId(Long farmerId) {
        String role = currentUserService.getCurrentRole();
        Long currentUserId = currentUserService.getCurrentUserId();

        if ("MANAGER".equals(role)) {
            return mapStops(routeStopRepository.findByFarmerId(farmerId));
        }

        if ("FARMER".equals(role)) {
            // Le FARMER ne peut demander que ses propres arrêts.
            if (!Objects.equals(farmerId, currentUserId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Vous ne pouvez consulter que vos propres arrêts"
                );
            }

            return mapStops(routeStopRepository.findByFarmerId(farmerId));
        }

        if ("DRIVER".equals(role)) {
            // Le DRIVER ne reçoit que les arrêts situés sur ses routes actives.
            List<RouteStop> stops = routeStopRepository.findByFarmerId(farmerId);

            return stops.stream()
                    .filter(stop -> stop.getRoute() != null)
                    .filter(stop -> hasActiveExecutionForDriver(
                            stop.getRoute().getId(),
                            currentUserId
                    ))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Rôle non autorisé pour cette opération"
        );
    }

    /**
     * Recherche globale par statut d'affectation.
     * Cette méthode est réservée au MANAGER.
     */
    public List<RouteStopResponse> getStopsByAssignmentStatus(
            AssignmentStatusRouteStop status
    ) {
        requireManager();

        return routeStopRepository.findByAssignmentStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // CREATION
    // =====================================================

    /**
     * Crée un arrêt.
     * routeId est facultatif.
     * sequenceOrder est interdit si aucune route n'est affectée.
     */
    public RouteStopResponse createStop(RouteStopRequest request) {
        requireManager();

        Route route = null;

        if (request.getRouteId() != null) {
            route = findRouteById(request.getRouteId());
        }

        if (route == null && request.getSequenceOrder() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une route doit être affectée avant de définir un ordre"
            );
        }

        RouteStop stop = RouteStop.builder()
                .route(route)
                .farmerId(request.getFarmerId())
                .sequenceOrder(request.getSequenceOrder())
                .plannedTime(request.getPlannedTime())
                .build();

        // Le statut devient ASSIGNED ou UNASSIGNED selon la présence d'une route.
        stop.updateAssignmentStatus();

        RouteStop savedStop = routeStopRepository.save(stop);
        return mapToResponse(savedStop);
    }

    // =====================================================
    // AFFECTATION
    // =====================================================

    /**
     * Affecte un arrêt à une route.
     * Cette opération modifie la planification et est réservée au MANAGER.
     */
    public RouteStopResponse assignStop(
            Long stopId,
            RouteStopAssignmentRequest request
    ) {
        requireManager();

        RouteStop stop = findStopById(stopId);
        Route route = findRouteById(request.getRouteId());

        stop.setRoute(route);
        stop.setSequenceOrder(request.getSequenceOrder());

        if (request.getPlannedTime() != null) {
            stop.setPlannedTime(request.getPlannedTime());
        }

        stop.updateAssignmentStatus();

        RouteStop updatedStop = routeStopRepository.save(stop);
        return mapToResponse(updatedStop);
    }

    /**
     * Désaffecte complètement un arrêt.
     */
    public RouteStopResponse unassignStop(Long stopId) {
        requireManager();

        RouteStop stop = findStopById(stopId);
        stop.unassignRoute();

        RouteStop updatedStop = routeStopRepository.save(stop);
        return mapToResponse(updatedStop);
    }

    // =====================================================
    // MODIFICATION
    // =====================================================

    /**
     * Modifie partiellement un arrêt.
     * Pour désaffecter un arrêt, il faut utiliser unassignStop().
     */
    public RouteStopResponse updateStop(
            Long stopId,
            RouteStopUpdateRequest request
    ) {
        requireManager();

        RouteStop stop = findStopById(stopId);

        if (request.getRouteId() != null) {
            Route route = findRouteById(request.getRouteId());
            stop.setRoute(route);
        }

        if (request.getSequenceOrder() != null) {
            if (stop.getRoute() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Une route doit être affectée avant de définir un ordre"
                );
            }

            stop.setSequenceOrder(request.getSequenceOrder());
        }

        if (request.getPlannedTime() != null) {
            stop.setPlannedTime(request.getPlannedTime());
        }

        stop.updateAssignmentStatus();

        RouteStop updatedStop = routeStopRepository.save(stop);
        return mapToResponse(updatedStop);
    }

    // =====================================================
    // SUPPRESSION
    // =====================================================

    /**
     * Supprime physiquement un arrêt.
     */
    public void deleteStop(Long stopId) {
        requireManager();

        RouteStop stop = findStopById(stopId);
        routeStopRepository.delete(stop);
    }

    // =====================================================
    // CONTROLES D'ACCES
    // =====================================================

    /**
     * Contrôle l'accès à un arrêt précis.
     */
    private void checkAccess(Long stopId, Long routeId) {
        String role = currentUserService.getCurrentRole();
        Long currentUserId = currentUserService.getCurrentUserId();

        if ("MANAGER".equals(role)) {
            return;
        }

        if ("DRIVER".equals(role)) {
            if (routeId == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Cet arrêt n'est affecté à aucune route"
                );
            }

            if (!hasActiveExecutionForDriver(routeId, currentUserId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Cet arrêt appartient à une route qui ne vous est pas affectée"
                );
            }

            return;
        }

        if ("FARMER".equals(role)) {
            RouteStop routeStop = findStopById(stopId);

            if (!Objects.equals(routeStop.getFarmerId(), currentUserId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Vous n'avez pas accès à cet arrêt"
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
     * Vérifie qu'un chauffeur possède une exécution ACTIVE pour une route.
     */
    private boolean hasActiveExecutionForDriver(
            Long routeId,
            Long currentUserId
    ) {
        List<RouteExecution> executions =
                routeExecutionRepository
                        .findByRoute_IdOrderByExecutionDateDesc(routeId);

        return executions.stream()
                .anyMatch(execution ->
                        execution.getStatus() == RouteExecutionStatus.ACTIVE
                                && execution.getActualDriver() != null
                                && Objects.equals(
                                execution.getActualDriver().getUserId(),
                                currentUserId
                        )
                );
    }

    /**
     * Vérifie qu'un chauffeur peut consulter une route.
     */
    private void checkRouteAccess(Long routeId) {
        String role = currentUserService.getCurrentRole();

        if ("MANAGER".equals(role)) {
            return;
        }

        if ("DRIVER".equals(role)) {
            Long currentUserId = currentUserService.getCurrentUserId();

            if (!hasActiveExecutionForDriver(routeId, currentUserId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Cette route ne vous est pas affectée"
                );
            }

            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Vous n'êtes pas autorisé à consulter les arrêts de cette route"
        );
    }

    /**
     * Réserve les opérations d'écriture et les recherches globales au MANAGER.
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
    // METHODES UTILITAIRES
    // =====================================================

    private List<RouteStopResponse> mapStops(List<RouteStop> stops) {
        return stops.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private RouteStop findStopById(Long id) {
        return routeStopRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun arrêt trouvé avec l'ID : " + id
                ));
    }

    private Route findRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucune route trouvée avec l'ID : " + id
                ));
    }

    private RouteStopResponse mapToResponse(RouteStop stop) {
        return RouteStopResponse.builder()
                .id(stop.getId())
                .routeId(
                        stop.getRoute() != null
                                ? stop.getRoute().getId()
                                : null
                )
                .farmerId(stop.getFarmerId())
                .sequenceOrder(stop.getSequenceOrder())
                .plannedTime(stop.getPlannedTime())
                .assignmentStatus(stop.getAssignmentStatus())
                .createdAt(stop.getCreatedAt())
                .updatedAt(stop.getUpdatedAt())
                .build();
    }
}
