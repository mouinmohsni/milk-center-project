package org.milkcenter.collectionservice.dto.request;


import jakarta.validation.constraints.*;
import lombok.*;
import org.milkcenter.collectionservice.enums.CollectionStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionValidationRequest {

    @NotNull(message = "Le nouveau statut est obligatoire")
    private CollectionStatus status;

    // Optionnel — utilisé uniquement si le Manager corrige la quantité
    @DecimalMin(value = "0.01", inclusive = true, message = "La quantité doit être supérieure à 0")
    private BigDecimal quantityLiters;

    @Size(max = 200, message = "Le motif ne doit pas dépasser 200 caractères")
    private String notes;
}
