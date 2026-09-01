package org.milkcenter.collectionservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.collectionservice.dto.request.CollectionValidationRequest;
import org.milkcenter.collectionservice.dto.request.MilkCollectionRequest;
import org.milkcenter.collectionservice.dto.response.MilkCollectionResponse;
import org.milkcenter.collectionservice.enums.CollectionStatus;
import org.milkcenter.collectionservice.model.MilkCollection;
import org.milkcenter.collectionservice.repository.MilkCollectionRepository;
import org.milkcenter.collectionservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilkCollectionService {

    private final MilkCollectionRepository collectionRepository;
    private final CurrentUserService currentUserService;

    private void checkCollectionOwnership(MilkCollection collection ) {
        String role = currentUserService.getCurrentRole();
        Long connectedUserId = currentUserService.getCurrentUserId();

        if ("DRIVER".equals(role) && !collection.getDriverUserId().equals(connectedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas l'auteur de cette collecte");
        }
    }

    public MilkCollectionResponse createCollection(MilkCollectionRequest request) {
        if (collectionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return mapToResponse(collectionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur idempotence")));
        }

        MilkCollection collection = MilkCollection.builder()
                .farmerId(request.getFarmerId())
                .driverUserId(currentUserService.getCurrentUserId())
                .routeStopId(request.getRouteStopId())
                .collectedAt(request.getCollectedAt())
                .quantityLiters(request.getQuantityLiters())
                .temperatureCelsius(request.getTemperatureCelsius())
                .qualityNotes(request.getQualityNotes())
                .notes(request.getNotes())
                .idempotencyKey(request.getIdempotencyKey())
                .status(CollectionStatus.PENDING)
                .correctionCount(0)
                .build();

        return mapToResponse(collectionRepository.save(collection));
    }

    public List<MilkCollectionResponse> getAllCollections() {
        return collectionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MilkCollectionResponse getCollectionById(Long id) {
        MilkCollection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collecte non trouvée"));
        checkCollectionOwnership(collection);
        return mapToResponse(collection);
    }

    public List<MilkCollectionResponse> getCollectionsByFarmerId(Long farmerId) {
        return collectionRepository.findByFarmerId(farmerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MilkCollectionResponse> getCollectionsByFarmerIdAndStatus(Long farmerId, CollectionStatus status) {
        return collectionRepository.findByFarmerIdAndStatus(farmerId, status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MilkCollectionResponse> getCollectionsByDriverId(Long driverUserId) {
        return collectionRepository.findByDriverUserId(driverUserId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MilkCollectionResponse> getCollectionsByRouteStopId(Long routeStopId) {
        return collectionRepository.findByRouteStopId(routeStopId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MilkCollectionResponse> getCollectionsByPeriod(Date start, Date end) {
        return collectionRepository.findByCollectedAtBetween(start, end).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MilkCollectionResponse> getLatestCollectionsByFarmerId(Long farmerId) {
        return collectionRepository.findLatestByFarmerId(farmerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MilkCollectionResponse validateCollection(Long id, CollectionValidationRequest request) {
        MilkCollection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collecte non trouvée"));

        if (collection.getStatus() != CollectionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette collecte a déjà été traitée");
        }

        collection.setStatus(request.getStatus());
        collection.setValidatorUserId(currentUserService.getCurrentUserId());
        collection.setValidationNotes(request.getValidationNotes());

        if (request.getNotes() != null) {
            collection.setNotes((collection.getNotes() == null ? "" : collection.getNotes()) + " | Validation: " + request.getNotes());
        }

        if (request.getStatus() == CollectionStatus.CORRECTED) {
            if (collection.getCorrectionCount() >= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum une seule correction autorisée");
            }
            if (request.getQuantityLiters() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nouvelle quantité est requise pour une correction");
            }
            collection.setQuantityLiters(request.getQuantityLiters());
            collection.setCorrectionCount(collection.getCorrectionCount() + 1);
            collection.setUpdatedByUserId(currentUserService.getCurrentUserId());
        }

        return mapToResponse(collectionRepository.save(collection));
    }

    public BigDecimal getTotalLitersByFarmerAndPeriod(Long farmerId, CollectionStatus status, Date start, Date end) {
        BigDecimal total = collectionRepository.sumQuantityByFarmerIdAndStatusAndPeriod(farmerId, status, start, end);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalAcceptedLitersByFarmer(Long farmerId) {
        BigDecimal total = collectionRepository.sumAcceptedQuantityByFarmerId(farmerId);
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<Map<String, Object>> getStatisticsByStatus(Long farmerId) {
        List<Object[]> stats = collectionRepository.countByFarmerIdGroupByStatus(farmerId);
        return stats.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("status", row[0]);
            map.put("count", row[1]);
            return map;
        }).collect(Collectors.toList());
    }

    private MilkCollectionResponse mapToResponse(MilkCollection collection) {
        return MilkCollectionResponse.builder()
                .id(collection.getId())
                .farmerId(collection.getFarmerId())
                .driverUserId(collection.getDriverUserId())
                .routeStopId(collection.getRouteStopId())
                .collectedAt(collection.getCollectedAt())
                .quantityLiters(collection.getQuantityLiters())
                .temperatureCelsius(collection.getTemperatureCelsius())
                .qualityNotes(collection.getQualityNotes())
                .status(collection.getStatus())
                .notes(collection.getNotes())
                .correctionCount(collection.getCorrectionCount() != null ? collection.getCorrectionCount() : 0)
                .updatedByUserId(collection.getUpdatedByUserId())
                .validatorUserId(collection.getValidatorUserId())
                .validationNotes(collection.getValidationNotes())
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .build();
    }
}
