package org.milkcenter.fleetservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.routeStop.*;
import org.milkcenter.fleetservice.dto.response.RouteStopResponse;
import org.milkcenter.fleetservice.enums.AssignmentStatusRouteStop;
import org.milkcenter.fleetservice.service.RouteStopService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/route-stops" )
@RequiredArgsConstructor
public class RouteStopController {

    private final RouteStopService routeStopService;

    @GetMapping
    public ResponseEntity<List<RouteStopResponse>> getAllStops() {
        return ResponseEntity.ok(routeStopService.getAllStops());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteStopResponse> getStopById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(routeStopService.getStopById(id));
    }

    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<RouteStopResponse>> getStopsByRouteId(
            @PathVariable Long routeId
    ) {
        return ResponseEntity.ok(
                routeStopService.getStopsByRouteId(routeId)
        );
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<RouteStopResponse>> getStopsByFarmerId(
            @PathVariable Long farmerId
    ) {
        return ResponseEntity.ok(
                routeStopService.getStopsByFarmerId(farmerId)
        );
    }

    @GetMapping("/assignment-status/{status}")
    public ResponseEntity<List<RouteStopResponse>>
    getStopsByAssignmentStatus(
            @PathVariable AssignmentStatusRouteStop status
    ) {
        return ResponseEntity.ok(
                routeStopService.getStopsByAssignmentStatus(status)
        );
    }

    @PostMapping
    public ResponseEntity<RouteStopResponse> createStop(
            @Valid @RequestBody RouteStopRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeStopService.createStop(request));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<RouteStopResponse> assignStop(
            @PathVariable Long id,
            @Valid @RequestBody RouteStopAssignmentRequest request
    ) {
        return ResponseEntity.ok(
                routeStopService.assignStop(id, request)
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RouteStopResponse> updateStop(
            @PathVariable Long id,
            @Valid @RequestBody RouteStopUpdateRequest request
    ) {
        return ResponseEntity.ok(
                routeStopService.updateStop(id, request)
        );
    }

    @PatchMapping("/{id}/unassign")
    public ResponseEntity<RouteStopResponse> unassignStop(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                routeStopService.unassignStop(id)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStop(
            @PathVariable Long id
    ) {
        routeStopService.deleteStop(id);
        return ResponseEntity.noContent().build();
    }
}
