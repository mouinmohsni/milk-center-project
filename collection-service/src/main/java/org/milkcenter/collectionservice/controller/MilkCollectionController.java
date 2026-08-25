package org.milkcenter.collectionservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.collectionservice.dto.request.CollectionValidationRequest;
import org.milkcenter.collectionservice.dto.request.MilkCollectionRequest;
import org.milkcenter.collectionservice.dto.response.MilkCollectionResponse;
import org.milkcenter.collectionservice.enums.CollectionStatus;
import org.milkcenter.collectionservice.service.MilkCollectionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collections" )
@RequiredArgsConstructor
public class MilkCollectionController {

    private final MilkCollectionService collectionService;

    // CREATE — POST /api/collections
    @PostMapping
    public ResponseEntity<MilkCollectionResponse> createCollection(
            @Valid @RequestBody MilkCollectionRequest request) {
        MilkCollectionResponse response = collectionService.createCollection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // READ — GET /api/collections
    @GetMapping
    public ResponseEntity<List<MilkCollectionResponse>> getAllCollections() {
        return ResponseEntity.ok(collectionService.getAllCollections());
    }

    // READ — GET /api/collections/{id}
    @GetMapping("/{id}")
    public ResponseEntity<MilkCollectionResponse> getCollectionById(@PathVariable Long id) {
        return ResponseEntity.ok(collectionService.getCollectionById(id));
    }

    // READ — GET /api/collections/farmer/{farmerId}
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<MilkCollectionResponse>> getByFarmerId(
            @PathVariable Long farmerId) {
        return ResponseEntity.ok(collectionService.getCollectionsByFarmerId(farmerId));
    }

    // READ — GET /api/collections/farmer/{farmerId}/status/{status}
    @GetMapping("/farmer/{farmerId}/status/{status}")
    public ResponseEntity<List<MilkCollectionResponse>> getByFarmerIdAndStatus(
            @PathVariable Long farmerId,
            @PathVariable CollectionStatus status) {
        return ResponseEntity.ok(
                collectionService.getCollectionsByFarmerIdAndStatus(farmerId, status)
        );
    }

    // READ — GET /api/collections/driver/{driverUserId}
    @GetMapping("/driver/{driverUserId}")
    public ResponseEntity<List<MilkCollectionResponse>> getByDriverId(
            @PathVariable Long driverUserId) {
        return ResponseEntity.ok(collectionService.getCollectionsByDriverId(driverUserId));
    }

    // READ — GET /api/collections/route-stop/{routeStopId}
    @GetMapping("/route-stop/{routeStopId}")
    public ResponseEntity<List<MilkCollectionResponse>> getByRouteStopId(
            @PathVariable Long routeStopId) {
        return ResponseEntity.ok(collectionService.getCollectionsByRouteStopId(routeStopId));
    }

    // READ — GET /api/collections/period?start=2026-08-01&end=2026-08-31
    @GetMapping("/period")
    public ResponseEntity<List<MilkCollectionResponse>> getByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date end) {
        return ResponseEntity.ok(collectionService.getCollectionsByPeriod(start, end));
    }

    // VALIDATE — PUT /api/collections/{id}/validate
    @PutMapping("/{id}/validate")
    public ResponseEntity<MilkCollectionResponse> validateCollection(
            @PathVariable Long id,
            @Valid @RequestBody CollectionValidationRequest request) {
        return ResponseEntity.ok(collectionService.validateCollection(id, request));
    }

    // STATISTICS — GET /api/collections/stats/total?farmerId=1&status=ACCEPTED&start=2026-08-01&end=2026-08-31
    @GetMapping("/stats/total")
    public ResponseEntity<Map<String, Object>> getTotalLiters(
            @RequestParam Long farmerId,
            @RequestParam CollectionStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date end) {
        BigDecimal total = collectionService.getTotalLitersByFarmerAndPeriod(
                farmerId, status, start, end
        );
        return ResponseEntity.ok(Map.of("totalLiters", total));
    }

    // STATISTICS — GET /api/collections/stats/accepted?farmerId=1
    @GetMapping("/stats/accepted")
    public ResponseEntity<Map<String, Object>> getTotalAcceptedLiters(
            @RequestParam Long farmerId) {
        BigDecimal total = collectionService.getTotalAcceptedLitersByFarmer(farmerId);
        return ResponseEntity.ok(Map.of("totalAcceptedLiters", total));
    }

    // STATISTICS — GET /api/collections/stats/by-status?farmerId=1
    @GetMapping("/stats/by-status")
    public ResponseEntity<List<Map<String, Object>>> getStatisticsByStatus(
            @RequestParam Long farmerId) {
        return ResponseEntity.ok(collectionService.getStatisticsByStatus(farmerId));
    }
}
