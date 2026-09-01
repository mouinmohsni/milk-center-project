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

    @NotNull(message = "Le statut est obligatoire")
    private CollectionStatus status;

    @DecimalMin(value = "0.01")
    private BigDecimal quantityLiters;

    @Size(max = 500)
    private String validationNotes;

    @Size(max = 200)
    private String notes;
}
