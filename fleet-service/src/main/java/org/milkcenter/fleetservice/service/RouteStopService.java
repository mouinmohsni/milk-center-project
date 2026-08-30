package org.milkcenter.fleetservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.routeStop.RouteStopAssignmentRequest;
import org.milkcenter.fleetservice.dto.request.routeStop.RouteStopRequest;
import org.milkcenter.fleetservice.dto.request.routeStop.RouteStopUpdateRequest;
import org.milkcenter.fleetservice.dto.response.RouteStopResponse;
import org.milkcenter.fleetservice.enums.AssignmentStatusRouteStop;
import org.milkcenter.fleetservice.model.Route;
import org.milkcenter.fleetservice.model.RouteStop;
import org.milkcenter.fleetservice.repository.RouteRepository;
import org.milkcenter.fleetservice.repository.RouteStopRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteStopService {

    private final RouteStopRepository routeStopRepository;
    private final RouteRepository routeRepository;

    // =====================================================
    // RECHERCHE
    // =====================================================

    /** Retourne tous les arrêts. */
    public List<RouteStopResponse> getAllStops( ) {
        return routeStopRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Recherche un arrêt par son identifiant. */
    public RouteStopResponse getStopById(Long id) {
        return mapToResponse(findStopById(id));
    }

    /** Retourne les arrêts d'une route donnée. */
    public List<RouteStopResponse> getStopsByRouteId(Long routeId) {
        // On vérifie d'abord que la route existe.
        findRouteById(routeId);

        return routeStopRepository.findByRoute_Id(routeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Retourne tous les arrêts d'un fermier. */
    public List<RouteStopResponse> getStopsByFarmerId(Long farmerId) {
        return routeStopRepository.findByFarmerId(farmerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Retourne les arrêts affectés ou non affectés. */
    public List<RouteStopResponse> getStopsByAssignmentStatus(
            AssignmentStatusRouteStop status
    ) {
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
     *
     * routeId est facultatif : l'arrêt peut être créé sans route.
     * En revanche, sequenceOrder ne peut pas être fourni sans route.
     */
    public RouteStopResponse createStop(RouteStopRequest request) {

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

        // assignmentStatus sera également calculé par @PrePersist.
        stop.updateAssignmentStatus();

        RouteStop savedStop = routeStopRepository.save(stop);
        return mapToResponse(savedStop);
    }

    // =====================================================
    // AFFECTATION
    // =====================================================

    /**
     * Affecte un arrêt à une route.
     *
     * L'ordre peut être null : le manager peut choisir la route
     * maintenant et définir la position plus tard.
     */
    public RouteStopResponse assignStop(
            Long stopId,
            RouteStopAssignmentRequest request
    ) {
        RouteStop stop = findStopById(stopId);
        Route route = findRouteById(request.getRouteId());

        stop.setRoute(route);
        stop.setSequenceOrder(request.getSequenceOrder());

        if (request.getPlannedTime() != null) {
            stop.setPlannedTime(request.getPlannedTime());
        }

        // La route existe : le statut devient ASSIGNED,
        // même si sequenceOrder est encore null.
        stop.updateAssignmentStatus();

        RouteStop updatedStop = routeStopRepository.save(stop);
        return mapToResponse(updatedStop);
    }

    /**
     * Désaffecte complètement un arrêt.
     *
     * La méthode de l'entité met route et sequenceOrder à null
     * et place assignmentStatus à UNASSIGNED.
     */
    public RouteStopResponse unassignStop(Long stopId) {
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
     *
     * Cette méthode ne désaffecte pas l'arrêt lorsque routeId est null.
     * Pour désaffecter, utilisez explicitement unassignStop().
     */
    public RouteStopResponse updateStop(
            Long stopId,
            RouteStopUpdateRequest request
    ) {
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
     *
     * À utiliser seulement si l'arrêt n'a pas d'historique métier.
     * Sinon, préférez unassignStop().
     */
    public void deleteStop(Long stopId) {
        RouteStop stop = findStopById(stopId);
        routeStopRepository.delete(stop);
    }

    // =====================================================
    // METHODES PRIVEES
    // =====================================================

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
