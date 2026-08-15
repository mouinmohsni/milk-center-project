package org.milkcenter.collectionservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.collectionservice.dto.request.CollectionValidationRequest;
import org.milkcenter.collectionservice.dto.request.MilkCollectionRequest;
import org.milkcenter.collectionservice.dto.response.MilkCollectionResponse;
import org.milkcenter.collectionservice.enums.CollectionStatus;
import org.milkcenter.collectionservice.model.MilkCollection;
import org.milkcenter.collectionservice.repository.MilkCollectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilkCollectionService {

    private final MilkCollectionRepository collectionRepository;

    // ============================================
    // CREATE — Enregistrer une nouvelle collecte
    // ============================================
    public MilkCollectionResponse createCollection(MilkCollectionRequest request ) {

        // Vérifier l'idempotence (éviter les doublons)
        if (collectionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette collecte a déjà été enregistrée (idempotency key: " + request.getIdempotencyKey() + ")"
            );
        }

        // Convertir le DTO en entité
        MilkCollection collection = MilkCollection.builder()
                .farmerId(request.getFarmerId())
                .driverUserId(request.getDriverUserId())
                .routeStopId(request.getRouteStopId())
                .collectedAt(request.getCollectedAt())
                .quantityLiters(request.getQuantityLiters())
                // Si le chauffeur n'envoie pas de statut, on met PENDING par défaut
                .status(request.getStatus() != null ? request.getStatus() : CollectionStatus.PENDING)
                .notes(request.getNotes())
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        // Sauvegarder en base
        MilkCollection saved = collectionRepository.save(collection);

        return mapToResponse(saved);
    }

    // ============================================
    // READ — Récupérer une collecte par ID
    // ============================================
    public MilkCollectionResponse getCollectionById(Long id) {
        MilkCollection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Collecte non trouvée avec ID: " + id
                ));
        return mapToResponse(collection);
    }

    // ============================================
    // READ — Lister toutes les collectes
    // ============================================
    public List<MilkCollectionResponse> getAllCollections() {
        return collectionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — Toutes les collectes d'un agriculteur
    // ============================================
    public List<MilkCollectionResponse> getCollectionsByFarmerId(Long farmerId) {
        return collectionRepository.findByFarmerId(farmerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — Collectes d'un agriculteur avec un statut précis
    // ============================================
    public List<MilkCollectionResponse> getCollectionsByFarmerIdAndStatus(
            Long farmerId, CollectionStatus status) {
        return collectionRepository.findByFarmerIdAndStatus(farmerId, status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — Toutes les collectes d'un chauffeur
    // ============================================
    public List<MilkCollectionResponse> getCollectionsByDriverId(Long driverUserId) {
        return collectionRepository.findByDriverUserId(driverUserId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — Collectes d'un arrêt de tournée
    // ============================================
    public List<MilkCollectionResponse> getCollectionsByRouteStopId(Long routeStopId) {
        return collectionRepository.findByRouteStopId(routeStopId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — Collectes entre deux dates
    // ============================================
    public List<MilkCollectionResponse> getCollectionsByPeriod(Date start, Date end) {
        return collectionRepository.findByCollectedAtBetween(start, end)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // READ — Collectes d'un agriculteur triées par date (plus récentes d'abord)
    // ============================================
    public List<MilkCollectionResponse> getLatestCollectionsByFarmerId(Long farmerId) {
        return collectionRepository.findLatestByFarmerId(farmerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // VALIDATE — Accepter, rejeter ou corriger une collecte
    // ============================================
    public MilkCollectionResponse validateCollection(Long id, CollectionValidationRequest request) {

        MilkCollection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Collecte non trouvée avec ID: " + id
                ));

        // Vérifier que la collecte est bien en PENDING (on ne peut valider qu'une fois)
        if (collection.getStatus() != CollectionStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La collecte a déjà été validée (statut actuel: " + collection.getStatus() + "). " +
                            "Seules les collectes PENDING peuvent être validées."
            );
        }

        // Appliquer l'action selon le statut demandé
        switch (request.getStatus()) {
            case ACCEPTED:
                collection.accept();
                break;

            case REJECTED:
                String reason = request.getNotes() != null ? request.getNotes() : "Aucun motif fourni";
                collection.reject(reason);
                break;

            case CORRECTED:
                // La quantité corrigée est obligatoire pour CORRECTED
                if (request.getQuantityLiters() == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "La quantité corrigée est obligatoire pour le statut CORRECTED"
                    );
                }
                collection.correctQuantity(request.getQuantityLiters());
                // Ajouter le motif en note si fourni
                if (request.getNotes() != null) {
                    String existingNotes = collection.getNotes() != null ? collection.getNotes() : "";
                    collection.setNotes(existingNotes + " [Correction: " + request.getNotes() + "]");
                }
                break;

            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Statut invalide pour la validation. Utilisez ACCEPTED, REJECTED ou CORRECTED"
                );
        }

        // Sauvegarder (le @PreUpdate mettra à jour updatedAt)
        MilkCollection updated = collectionRepository.save(collection);
        return mapToResponse(updated);
    }

    // ============================================
    // STATISTICS — Total de litres sur une période avec un statut donné
    // ============================================
    public BigDecimal getTotalLitersByFarmerAndPeriod(
            Long farmerId, CollectionStatus status, Date start, Date end) {
        BigDecimal total = collectionRepository.sumQuantityByFarmerIdAndStatusAndPeriod(
                farmerId, status, start, end
        );
        // SUM() retourne null si aucun résultat → convertir en 0
        return total != null ? total : BigDecimal.ZERO;
    }

    // ============================================
    // STATISTICS — Total de litres ACCEPTED (tout temps)
    // ============================================
    public BigDecimal getTotalAcceptedLitersByFarmer(Long farmerId) {
        BigDecimal total = collectionRepository.sumAcceptedQuantityByFarmerId(farmerId);
        return total != null ? total : BigDecimal.ZERO;
    }

    // ============================================
    // STATISTICS — Répartition des collectes par statut (pour un agriculteur)
    // ============================================
    public List<Map<String, Object>> getStatisticsByStatus(Long farmerId) {
        List<Object[]> rows = collectionRepository.countByFarmerIdGroupByStatus(farmerId);

        return rows.stream()
                .map(row -> Map.of(
                        "status", row[0].toString(),      // CollectionStatus en String
                        "count", row[1]                   // Nombre de collectes
                ))
                .collect(Collectors.toList());
    }

    // ============================================
    // STATISTICS — Première collecte dépassant une quantité
    // ============================================
    public MilkCollectionResponse findFirstCollectionAboveQuantity(
            Long farmerId, BigDecimal quantityLiters) {
        MilkCollection collection = collectionRepository.findFirstBySupQuantityLiters(
                farmerId, quantityLiters
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Aucune collecte dépassant " + quantityLiters + " litres trouvée pour cet agriculteur"
        ));
        return mapToResponse(collection);
    }

    // ============================================
    // Mapper privé — Entité → DTO Response
    // ============================================
    private MilkCollectionResponse mapToResponse(MilkCollection collection) {
        return MilkCollectionResponse.builder()
                .id(collection.getId())
                .farmerId(collection.getFarmerId())
                .driverUserId(collection.getDriverUserId())
                .routeStopId(collection.getRouteStopId())
                .collectedAt(collection.getCollectedAt())
                .quantityLiters(collection.getQuantityLiters())
                .status(collection.getStatus())
                .notes(collection.getNotes())
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .build();
    }
}
