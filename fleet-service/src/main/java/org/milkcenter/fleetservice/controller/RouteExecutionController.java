package org.milkcenter.fleetservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.routeExecution.*;
import org.milkcenter.fleetservice.dto.response.RouteExecutionResponse;
import org.milkcenter.fleetservice.service.RouteExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route-executions" )
@RequiredArgsConstructor
public class RouteExecutionController {

    private final RouteExecutionService routeExecutionService;

    @GetMapping
    public ResponseEntity<List<RouteExecutionResponse>>
    getAllExecutions() {
        return ResponseEntity.ok(
                routeExecutionService.getAllExecutions()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteExecutionResponse> getExecutionById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                routeExecutionService.getExecutionById(id)
        );
    }

    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<RouteExecutionResponse>>
    getExecutionsByRouteId(
            @PathVariable Long routeId
    ) {
        return ResponseEntity.ok(
                routeExecutionService.getExecutionsByRouteId(routeId)
        );
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<RouteExecutionResponse>>
    getExecutionsByDriverId(
            @PathVariable Long driverId
    ) {
        return ResponseEntity.ok(
                routeExecutionService.getExecutionsByDriverId(driverId)
        );
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<RouteExecutionResponse>>
    getExecutionsByVehicleId(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(
                routeExecutionService.getExecutionsByVehicleId(vehicleId)
        );
    }

    @PostMapping
    public ResponseEntity<RouteExecutionResponse> createExecution(
            @Valid @RequestBody RouteExecutionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeExecutionService.createExecution(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RouteExecutionResponse> updateExecution(
            @PathVariable Long id,
            @Valid @RequestBody RouteExecutionUpdateRequest request
    ) {
        return ResponseEntity.ok(
                routeExecutionService.updateExecution(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RouteExecutionResponse> updateExecutionStatus(
            @PathVariable Long id,
            @Valid @RequestBody RouteExecutionStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                routeExecutionService.updateExecutionStatus(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExecution(
            @PathVariable Long id
    ) {
        routeExecutionService.deleteExecution(id);
        return ResponseEntity.noContent().build();
    }
}
