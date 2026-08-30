package org.milkcenter.fleetservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.route.RouteRequest;
import org.milkcenter.fleetservice.dto.request.route.RouteStatusUpdateRequest;
import org.milkcenter.fleetservice.dto.request.route.RouteUpdateRequest;
import org.milkcenter.fleetservice.dto.response.RouteResponse;
import org.milkcenter.fleetservice.enums.RouteStatus;
import org.milkcenter.fleetservice.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/routes" )
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getRouteById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<RouteResponse>> getRoutesByDriverId(
            @PathVariable Long driverId
    ) {
        return ResponseEntity.ok(
                routeService.getRoutesByDriverId(driverId)
        );
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<RouteResponse>> getRoutesByVehicleId(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(
                routeService.getRoutesByVehicleId(vehicleId)
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<RouteResponse>> getRoutesByStatus(
            @PathVariable RouteStatus status
    ) {
        return ResponseEntity.ok(
                routeService.getRoutesByStatus(status)
        );
    }

    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(
            @Valid @RequestBody RouteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.createRoute(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RouteResponse> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody RouteUpdateRequest request
    ) {
        return ResponseEntity.ok(
                routeService.updateRoute(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RouteResponse> updateRouteStatus(
            @PathVariable Long id,
            @Valid @RequestBody RouteStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                routeService.updateRouteStatus(id, request)
        );
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<RouteResponse> activateRoute(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(routeService.activateRoute(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RouteResponse> cancelRoute(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(routeService.cancelRoute(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(
            @PathVariable Long id
    ) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
