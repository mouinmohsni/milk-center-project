package org.milkcenter.collectionservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.collectionservice.dto.request.FarmerProfileRequest;
import org.milkcenter.collectionservice.dto.response.FarmerProfileResponse;
import org.milkcenter.collectionservice.model.FarmerProfile;
import org.milkcenter.collectionservice.repository.FarmerRepository;
import org.milkcenter.collectionservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final CurrentUserService currentUserService;

    private void checkFarmerOwnership(FarmerProfile farmer ) {
        String role = currentUserService.getCurrentRole();
        Long connectedUserId = currentUserService.getCurrentUserId();

        if ("FARMER".equals(role) && !farmer.getUserId().equals(connectedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous n'avez pas le droit d'accéder à ce profil"
            );
        }
    }

    public List<FarmerProfileResponse> getAllFarmer() {
        return farmerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public FarmerProfileResponse getOneFarmer(Long id) {
        FarmerProfile farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agriculteur non trouvé"));
        checkFarmerOwnership(farmer);
        return mapToResponse(farmer);
    }

    public FarmerProfileResponse findByUserId(Long userId) {
        FarmerProfile farmer = farmerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil agriculteur non trouvé"));
        checkFarmerOwnership(farmer);
        return mapToResponse(farmer);
    }

    public FarmerProfileResponse findByFarmName(String farmName) {
        FarmerProfile farmer = farmerRepository.findByFarmName(farmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ferme non trouvée"));
        return mapToResponse(farmer);
    }

    public List<FarmerProfileResponse> findByAddress(String address) {
        // Adaptation pour retourner une liste comme attendu par le contrôleur
        return farmerRepository.findByAddress(address).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public FarmerProfileResponse createFarmer(FarmerProfileRequest request) {
        if (farmerRepository.existsByUserId(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un profil existe déjà pour cet utilisateur");
        }

        FarmerProfile farmer = FarmerProfile.builder()
                .userId(request.getUserId())
                .farmName(request.getFarmName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .herdSize(request.getHerdSize())
                .active(true)
                .build();

        return mapToResponse(farmerRepository.save(farmer));
    }

    public FarmerProfileResponse updateFarmer(Long id, FarmerProfileRequest request) {
        FarmerProfile farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agriculteur non trouvé"));
        checkFarmerOwnership(farmer);

        farmer.setFarmName(request.getFarmName());
        farmer.setAddress(request.getAddress());
        farmer.setLatitude(request.getLatitude());
        farmer.setLongitude(request.getLongitude());
        farmer.setHerdSize(request.getHerdSize());

        return mapToResponse(farmerRepository.save(farmer));
    }

    public FarmerProfileResponse deactivateFarmer(Long id) {
        FarmerProfile farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agriculteur non trouvé"));
        farmer.setActive(false);
        return mapToResponse(farmerRepository.save(farmer));
    }

    private FarmerProfileResponse mapToResponse(FarmerProfile farmer) {
        return FarmerProfileResponse.builder()
                .id(farmer.getId())
                .userId(farmer.getUserId())
                .farmName(farmer.getFarmName())
                .address(farmer.getAddress())
                .latitude(farmer.getLatitude())
                .longitude(farmer.getLongitude())
                .herdSize(farmer.getHerdSize())
                .active(farmer.isActive())
                .createdAt(farmer.getCreatedAt())
                .updatedAt(farmer.getUpdatedAt())
                .build();
    }
}
