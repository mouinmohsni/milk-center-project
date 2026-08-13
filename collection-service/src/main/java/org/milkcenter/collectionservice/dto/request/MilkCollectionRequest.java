package org.milkcenter.collectionservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.milkcenter.collectionservice.enums.CollectionStatus;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkCollectionRequest {

    @NotNull(message = "Le farmerId est obligatoire")
    private Long farmerId;

    private Long driverUserId;      // Optionnel — sera rempli par le Manager

    private Long routeStopId;       // Optionnel — sera lié à Fleet & Ops

    @NotNull(message = "La date de collecte est obligatoire")
    @PastOrPresent(message = "La date de collecte ne peut pas être dans le futur")
    private Date collectedAt;

    @NotNull(message = "La quantité est obligatoire")
    @DecimalMin(value = "0.01", inclusive = true, message = "La quantité doit être supérieure à 0")
    @DecimalMax(value = "999999.99", message = "La quantité est trop élevée")
    private BigDecimal quantityLiters;

    @Builder.Default
    private CollectionStatus status = CollectionStatus.PENDING;

    @Size(max = 500, message = "Les notes ne doivent pas dépasser 500 caractères")
    private String notes;

    @NotBlank(message = "La clé d'unicité est obligatoire")
    @Size(max = 64, message = "La clé d'unicité ne doit pas dépasser 64 caractères")
    private String idempotencyKey;
}
