package org.milkcenter.collectionservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkCollectionRequest {

    @NotNull(message = "Le farmerId est obligatoire")
    private Long farmerId;

    private Long routeStopId;

    @NotNull(message = "La date de collecte est obligatoire")
    @PastOrPresent(message = "La date de collecte ne peut pas être dans le futur")
    private Date collectedAt;

    @NotNull(message = "La quantité est obligatoire")
    @DecimalMin(value = "0.01", message = "La quantité doit être supérieure à 0")
    private BigDecimal quantityLiters;

    @DecimalMin(value = "-50.00", message = "Température trop basse")
    @DecimalMax(value = "100.00", message = "Température trop haute")
    private BigDecimal temperatureCelsius;

    @Size(max = 500)
    private String qualityNotes;

    @Size(max = 500)
    private String notes;

    @NotBlank(message = "La clé d'unicité est obligatoire")
    @Size(max = 64)
    private String idempotencyKey;
}
