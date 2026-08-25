package org.milkcenter.collectionservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.collectionservice.dto.request.FarmerProfileRequest;
import org.milkcenter.collectionservice.dto.response.FarmerProfileResponse;
import org.milkcenter.collectionservice.service.FarmerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farmers" )
@RequiredArgsConstructor
public class FarmerController {

    private final FarmerService farmerService;

    // CREATE — POST /api/farmers
    @PostMapping
    public ResponseEntity<FarmerProfileResponse> createFarmer(
            @Valid @RequestBody FarmerProfileRequest request) {
        FarmerProfileResponse response = farmerService.createFarmer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // READ — GET /api/farmers
    @GetMapping
    public ResponseEntity<List<FarmerProfileResponse>> getAllFarmers() {
        return ResponseEntity.ok(farmerService.getAllFarmer());
    }

    // READ — GET /api/farmers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<FarmerProfileResponse> getFarmerById(@PathVariable Long id) {
        return ResponseEntity.ok(farmerService.getOneFarmer(id));
    }

    // READ — GET /api/farmers/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<FarmerProfileResponse> getFarmerByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(farmerService.findByUserId(userId));
    }

    // READ — GET /api/farmers/search/name?farmName=...
    @GetMapping("/search/name")
    public ResponseEntity<FarmerProfileResponse> getFarmerByFarmName(
            @RequestParam String farmName) {
        return ResponseEntity.ok(farmerService.findByFarmName(farmName));
    }

    // READ — GET /api/farmers/search/address?address=...
    @GetMapping("/search/address")
    public ResponseEntity<List<FarmerProfileResponse>> getFarmersByAddress(
            @RequestParam String address) {
        return ResponseEntity.ok(farmerService.findByAddress(address));
    }

    // UPDATE — PUT /api/farmers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<FarmerProfileResponse> updateFarmer(
            @PathVariable Long id,
            @Valid @RequestBody FarmerProfileRequest request) {
        return ResponseEntity.ok(farmerService.updateFarmer(id, request));
    }

    // DELETE (soft) — PATCH /api/farmers/{id}/deactivate
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<FarmerProfileResponse> deactivateFarmer(@PathVariable Long id) {
        return ResponseEntity.ok(farmerService.deactivateFarmer(id));
    }
}
