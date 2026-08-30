package org.milkcenter.fleetservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.route.RouteRequest;
import org.milkcenter.fleetservice.dto.request.route.RouteStatusUpdateRequest;
import org.milkcenter.fleetservice.dto.response.RouteResponse;

import org.milkcenter.fleetservice.enums.RouteStatus;

import org.milkcenter.fleetservice.model.Driver;
import org.milkcenter.fleetservice.model.Route;
import org.milkcenter.fleetservice.model.RouteStop;
import org.milkcenter.fleetservice.model.Vehicle;
import org.milkcenter.fleetservice.repository.DriverRepository;
import org.milkcenter.fleetservice.repository.RouteRepository;
import org.milkcenter.fleetservice.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteService {

    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    // =====================================================
    // RECHERCHE
    // =====================================================

    /**
     * Retourne toutes les routes avec leurs informations principales.
     */
    public List<RouteResponse> getAllRoutes( ) {
        return routeRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recherche une route par son identifiant.
     */
    public RouteResponse getRouteById(Long id) {
        return mapToResponse(findRouteById(id));
    }

    /**
     * Retourne les routes habituelles d'un chauffeur.
     */
    public List<RouteResponse> getRoutesByDriverId(Long driverId) {
        return routeRepository.findByDriverId(driverId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les routes associées à un véhicule.
     */
    public List<RouteResponse> getRoutesByVehicleId(Long vehicleId) {
        return routeRepository.findByVehicleId(vehicleId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les routes selon leur statut : PLANNED, ACTIVE ou CANCELLED.
     */
    public List<RouteResponse> getRoutesByStatus(RouteStatus status) {
        return routeRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // CREATION
    // =====================================================

    /**
     * Crée une route réutilisable.
     * La route possède une affectation habituelle vers un chauffeur
     * et un véhicule.
     */
    public RouteResponse createRoute(RouteRequest request) {


        Driver driver = findDriverById(request.getDriverId());
        Vehicle vehicle = findVehicleById(request.getVehicleId());

        Route route = Route.builder()
                .name(request.getName())
                .driver(driver)
                .vehicle(vehicle)
                .plannedDate(request.getPlannedDate())
                .status(
                        request.getStatus() != null
                                ? request.getStatus()
                                : RouteStatus.PLANNED
                )
                .stops(new ArrayList<>())
                .build();

        /*
         * Les arrêts sont facultatifs.
         * Si le manager les envoie avec la route, on les rattache ici.
         */
        if (request.getStops() != null) {
            request.getStops().forEach(stopRequest -> {
                RouteStop stop = RouteStop.builder()
                        .route(route)
                        .farmerId(stopRequest.getFarmerId())
                        .sequenceOrder(stopRequest.getSequenceOrder())
                        .plannedTime(stopRequest.getPlannedTime())
                        .build();

                route.getStops().add(stop);
            });
        }

        Route savedRoute = routeRepository.save(route);

        return mapToResponse(savedRoute);
    }

    // =====================================================
    // MODIFICATION GENERALE
    // =====================================================

    /**
     * Modifie les informations générales de la route.
     */
    public RouteResponse updateRoute(
            Long id,
            RouteRequest request
    ) {
        Route route = findRouteById(id);

        Driver driver = findDriverById(request.getDriverId());
        Vehicle vehicle = findVehicleById(request.getVehicleId());

        route.setName(request.getName());
        route.setDriver(driver);
        route.setVehicle(vehicle);
        route.setPlannedDate(request.getPlannedDate());


        Route updatedRoute = routeRepository.save(route);
        return mapToResponse(updatedRoute);
    }

    // =====================================================
    // MODIFICATION DU STATUT
    // =====================================================

    /**
     * Modifie uniquement le statut de la route.
     */
    public RouteResponse updateRouteStatus(
            Long id,
            RouteStatusUpdateRequest request
    ) {
        Route route = findRouteById(id);

        if (request.getStatus() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le statut de la route est obligatoire"
            );
        }

        route.setStatus(request.getStatus());

        Route updatedRoute = routeRepository.save(route);
        return mapToResponse(updatedRoute);
    }

    /**
     * Annule une route sans supprimer ses données.
     */
    public RouteResponse cancelRoute(Long id) {
        Route route = findRouteById(id);

        route.setStatus(RouteStatus.CANCELLED);

        Route updatedRoute = routeRepository.save(route);
        return mapToResponse(updatedRoute);
    }

    /**
     * Active une route préparée.
     */
    public RouteResponse activateRoute(Long id) {
        Route route = findRouteById(id);

        route.setStatus(RouteStatus.ACTIVE);

        Route updatedRoute = routeRepository.save(route);
        return mapToResponse(updatedRoute);
    }

    // =====================================================
    // VALIDATIONS METIER
    // =====================================================

    /**
     * Vérifie qu'un chauffeur existe avant son affectation à une route.
     */
    public void validateDriverForRoute(Long driverId) {
        findDriverById(driverId);
    }

    /**
     * Vérifie qu'un véhicule existe avant son affectation à une route.
     */
    public void validateVehicleForRoute(Long vehicleId) {
        findVehicleById(vehicleId);
    }

    /**
     * Vérifie qu'une route peut être utilisée.
     */
    public void validateRouteIsActive(Long routeId) {
        Route route = findRouteById(routeId);

        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La route avec l'ID " + routeId
                            + " n'est pas active. Statut actuel : "
                            + route.getStatus()
            );
        }
    }

    // =====================================================
    // SUPPRESSION
    // =====================================================

    /**
     * Suppression physique exceptionnelle.
     */
    public void deleteRoute(Long id) {
        Route route = findRouteById(id);
        routeRepository.delete(route);
    }

    // =====================================================
    // METHODES PRIVEES
    // =====================================================

    /**
     * Recherche une route ou retourne une erreur 404.
     */
    private Route findRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucune route trouvée avec l'ID : " + id
                ));
    }

    /**
     * Recherche un chauffeur ou retourne une erreur 404.
     */
    private Driver findDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun chauffeur trouvé avec l'ID : " + id
                ));
    }

    /**
     * Recherche un véhicule ou retourne une erreur 404.
     */
    private Vehicle findVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun véhicule trouvé avec l'ID : " + id
                ));
    }

    /**
     * Transforme une entité Route en RouteResponse.
     * On retourne uniquement les identifiants des relations.
     */
    private RouteResponse mapToResponse(Route route) {
        List<Long> stopIds = route.getStops() == null
                ? new ArrayList<>()
                : route.getStops()
                .stream()
                .map(RouteStop::getId)
                .collect(Collectors.toList());

        return RouteResponse.builder()
                .id(route.getId())
                .name(route.getName())
                .driverId(
                        route.getDriver() != null
                                ? route.getDriver().getId()
                                : null
                )
                .vehicleId(
                        route.getVehicle() != null
                                ? route.getVehicle().getId()
                                : null
                )
                .plannedDate(route.getPlannedDate())
                .status(route.getStatus())
                .stops(stopIds)
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
