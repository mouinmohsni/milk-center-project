package org.milkcenter.collectionservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.collectionservice.dto.request.FarmerProfileRequest;
import org.milkcenter.collectionservice.dto.response.FarmerProfileResponse;
import org.milkcenter.collectionservice.model.FarmerProfile;
import org.milkcenter.collectionservice.repository.FarmerRepository;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class FarmerService {

    private final FarmerRepository farmerRepository ;


    // ============================================
    // READ — Récupérer tous les agriculteurs
    // ============================================
    public List<FarmerProfileResponse> getAllFarmer() {
        return farmerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — Récupérer tous les agriculteurs actifs
    // ============================================
    public List<FarmerProfileResponse> getAllFarmerActive() {
        return farmerRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — Récupérer un agriculteur par ID
    // ============================================
    public FarmerProfileResponse  getOneFarmer(Long id){
        FarmerProfile farmer = farmerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Agriculteur non trouvé avec  ID: " + id
        ));
        return mapToResponse(farmer);
    }

    // ============================================
    // READ — Récupérer un agriculteur par user ID
    // ============================================
    public FarmerProfileResponse  findByUserId(Long userId){

        FarmerProfile farmer = farmerRepository.findByUserId(userId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Agriculteur non trouvé avec  user ID: " + userId
        ));
        return mapToResponse(farmer);
    }

    // ============================================
    // READ — Récupérer un agriculteur par farmName
    // ============================================
    public FarmerProfileResponse findByFarmName(String farmName){
        FarmerProfile farmer = farmerRepository.findByFarmName(farmName).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Agriculteur non trouvé avec  le nom: " + farmName
        ));
        return mapToResponse(farmer);
    }


    // ============================================
    // READ — Récupérer un agriculteur par adresse
    // ============================================
    public List<FarmerProfileResponse> findByAddress(String address) {
        return farmerRepository.findByAddress(address)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // CREATE — Créer un nouveau profil agriculteur
    // ============================================
    public FarmerProfileResponse createFarmer(FarmerProfileRequest request ) {

        // Vérifier si un profil existe déjà pour ce userId
        if (farmerRepository.existsByUserId(request.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un profil existe déjà pour l'utilisateur ID: " + request.getUserId()
            );
        }

        // Convertir le DTO en entité
        FarmerProfile farmer = FarmerProfile.builder()
                .userId(request.getUserId())
                .farmName(request.getFarmName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .herdSize(request.getHerdSize())
                .build();

        // Sauvegarder en base
        FarmerProfile saved = farmerRepository.save(farmer);

        // Retourner le DTO Response
        return mapToResponse(saved);
    }

    // ============================================
    // UPDATE — Modifier un profil agriculteur
    // ============================================
    public FarmerProfileResponse updateFarmer(Long id, FarmerProfileRequest request) {

        FarmerProfile farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Agriculteur non trouvé avec ID: " + id
                ));

        // Vérifier que le userId n'est pas changé vers un autre agriculteur existant
        if (!farmer.getUserId().equals(request.getUserId())) {
            if (farmerRepository.existsByUserId(request.getUserId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Le userId " + request.getUserId() + " est déjà utilisé par un autre agriculteur"
                );
            }
        }

        // Mettre à jour les champs
        farmer.setFarmName(request.getFarmName());
        farmer.setAddress(request.getAddress());
        farmer.setLatitude(request.getLatitude());
        farmer.setLongitude(request.getLongitude());
        farmer.setHerdSize(request.getHerdSize());
        // Ne pas toucher à userId, active, createdAt

        // Sauvegarder (le @PreUpdate mettra à jour updatedAt automatiquement)
        FarmerProfile updated = farmerRepository.save(farmer);

        return mapToResponse(updated);
    }

    // ============================================
    // DELETE (soft) — Désactiver un agriculteur
    // ============================================
    public FarmerProfileResponse deactivateFarmer(Long id) {
        FarmerProfile farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Agriculteur non trouvé avec ID: " + id
                ));

        farmer.setActive(false);
        FarmerProfile updated = farmerRepository.save(farmer);

        return mapToResponse(updated);
    }

    // ============================================
    // Mapper privé — Entité → DTO Response
    // ============================================
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
