package org.milkcenter.collectionservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.collectionservice.dto.request.FarmerProfileRequest;
import org.milkcenter.collectionservice.dto.response.FarmerProfileResponse;
import org.milkcenter.collectionservice.security.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<FarmerProfileResponse> createFarmer(
            @Valid @RequestBody FarmerProfileRequest request) {
        FarmerProfileResponse response =
                farmerService.createFarmer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<FarmerProfileResponse> getMyProfile() {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(farmerService.findByUserId(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<FarmerProfileResponse> updateMyProfile(
            @Valid @RequestBody FarmerProfileRequest request) {

        Long userId = currentUserService.getCurrentUserId();
        FarmerProfileResponse currentProfile =
                farmerService.findByUserId(userId);

        return ResponseEntity.ok(
                farmerService.updateFarmer(
                        currentProfile.getId(),
                        request
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<FarmerProfileResponse>> getAllFarmers() {
        return ResponseEntity.ok(farmerService.getAllFarmer());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FarmerProfileResponse> getFarmerById(
            @PathVariable Long id) {
        return ResponseEntity.ok(farmerService.getOneFarmer(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<FarmerProfileResponse> getFarmerByUserId(
            @PathVariable Long userId) {
        return ResponseEntity.ok(farmerService.findByUserId(userId));
    }

    @GetMapping("/search/name")
    public ResponseEntity<FarmerProfileResponse> getFarmerByFarmName(
            @RequestParam String farmName) {
        return ResponseEntity.ok(farmerService.findByFarmName(farmName));
    }

    @GetMapping("/search/address")
    public ResponseEntity<List<FarmerProfileResponse>> getFarmersByAddress(
            @RequestParam String address) {
        return ResponseEntity.ok(farmerService.findByAddress(address));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FarmerProfileResponse> updateFarmer(
            @PathVariable Long id,
            @Valid @RequestBody FarmerProfileRequest request) {
        return ResponseEntity.ok(
                farmerService.updateFarmer(id, request)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<FarmerProfileResponse> deactivateFarmer(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                farmerService.deactivateFarmer(id)
        );
    }
}
