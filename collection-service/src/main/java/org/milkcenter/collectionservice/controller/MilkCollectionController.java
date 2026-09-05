package org.milkcenter.collectionservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.collectionservice.dto.request.CollectionValidationRequest;
import org.milkcenter.collectionservice.dto.request.MilkCollectionRequest;
import org.milkcenter.collectionservice.dto.response.FarmerProfileResponse;
import org.milkcenter.collectionservice.dto.response.MilkCollectionResponse;
import org.milkcenter.collectionservice.dto.response.MonthlyMilkTotalResponse;
import org.milkcenter.collectionservice.enums.CollectionStatus;
import org.milkcenter.collectionservice.security.CurrentUserService;
import org.milkcenter.collectionservice.service.FarmerService;
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
    private final FarmerService farmerService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<MilkCollectionResponse> createCollection(
            @Valid @RequestBody MilkCollectionRequest request) {

        MilkCollectionResponse response =
                collectionService.createCollection(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MilkCollectionResponse>> getAllCollections() {
        return ResponseEntity.ok(collectionService.getAllCollections());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MilkCollectionResponse> getCollectionById(
            @PathVariable Long id) {
        return ResponseEntity.ok(collectionService.getCollectionById(id));
    }

    // Le Driver consulte uniquement ses propres collectes
    @GetMapping("/driver/me")
    public ResponseEntity<List<MilkCollectionResponse>> getMyDriverCollections() {
        Long driverUserId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(
                collectionService.getCollectionsByDriverId(driverUserId)
        );
    }

    // Le Farmer consulte uniquement ses propres collectes
    @GetMapping("/farmer/me")
    public ResponseEntity<List<MilkCollectionResponse>> getMyFarmerCollections() {
        Long userId = currentUserService.getCurrentUserId();
        FarmerProfileResponse farmer = farmerService.findByUserId(userId);

        return ResponseEntity.ok(
                collectionService.getCollectionsByFarmerId(farmer.getId())
        );
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<MilkCollectionResponse>> getByFarmerId(
            @PathVariable Long farmerId) {
        return ResponseEntity.ok(
                collectionService.getCollectionsByFarmerId(farmerId)
        );
    }

    @GetMapping("/farmer/{farmerId}/status/{status}")
    public ResponseEntity<List<MilkCollectionResponse>> getByFarmerIdAndStatus(
            @PathVariable Long farmerId,
            @PathVariable CollectionStatus status) {
        return ResponseEntity.ok(
                collectionService.getCollectionsByFarmerIdAndStatus(
                        farmerId,
                        status
                )
        );
    }

    @GetMapping("/driver/{driverUserId}")
    public ResponseEntity<List<MilkCollectionResponse>> getByDriverId(
            @PathVariable Long driverUserId) {
        return ResponseEntity.ok(
                collectionService.getCollectionsByDriverId(driverUserId)
        );
    }

    @GetMapping("/route-stop/{routeStopId}")
    public ResponseEntity<List<MilkCollectionResponse>> getByRouteStopId(
            @PathVariable Long routeStopId) {
        return ResponseEntity.ok(
                collectionService.getCollectionsByRouteStopId(routeStopId)
        );
    }

    @GetMapping("/period")
    public ResponseEntity<List<MilkCollectionResponse>> getByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Date start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Date end) {
        return ResponseEntity.ok(
                collectionService.getCollectionsByPeriod(start, end)
        );
    }

    @PutMapping("/{id}/validate")
    public ResponseEntity<MilkCollectionResponse> validateCollection(
            @PathVariable Long id,
            @Valid @RequestBody CollectionValidationRequest request) {
        return ResponseEntity.ok(
                collectionService.validateCollection(id, request)
        );
    }

    @GetMapping("/stats/total")
    public ResponseEntity<Map<String, Object>> getTotalLiters(
            @RequestParam Long farmerId,
            @RequestParam CollectionStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Date start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Date end) {
        BigDecimal total = collectionService
                .getTotalLitersByFarmerAndPeriod(
                        farmerId, status, start, end
                );
        return ResponseEntity.ok(Map.of("totalLiters", total));
    }

    @GetMapping("/stats/accepted")
    public ResponseEntity<Map<String, Object>> getTotalAcceptedLiters(
            @RequestParam Long farmerId) {
        BigDecimal total = collectionService
                .getTotalAcceptedLitersByFarmer(farmerId);
        return ResponseEntity.ok(
                Map.of("totalAcceptedLiters", total)
        );
    }

    @GetMapping("/stats/by-status")
    public ResponseEntity<List<Map<String, Object>>> getStatisticsByStatus(
            @RequestParam Long farmerId) {
        return ResponseEntity.ok(
                collectionService.getStatisticsByStatus(farmerId)
        );
    }

    /**
     * Retourne le total de lait ACCEPTED d'un fermier pour un mois donné.
     * Cette API est destinée au calcul des factures mensuelles.
     */
    @GetMapping("/farmer/{farmerId}/monthly-total")
    public ResponseEntity<MonthlyMilkTotalResponse> getMonthlyAcceptedMilkTotal(
            @PathVariable Long farmerId,
            @RequestParam int month,
            @RequestParam int year
    ) {
        BigDecimal totalQuantityLiters =
                collectionService.getMonthlyAcceptedLiters(
                        farmerId,
                        month,
                        year
                );

        MonthlyMilkTotalResponse response = MonthlyMilkTotalResponse.builder()
                .farmerId(farmerId)
                .month(month)
                .year(year)
                .status(CollectionStatus.ACCEPTED)
                .totalQuantityLiters(totalQuantityLiters)
                .build();

        return ResponseEntity.ok(response);
    }

}
