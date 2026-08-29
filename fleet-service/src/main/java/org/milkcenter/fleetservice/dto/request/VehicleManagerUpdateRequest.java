package org.milkcenter.fleetservice.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;




@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleManagerUpdateRequest {

    @NotBlank(message = "La plaque d'immatriculation est obligatoire")
    private String licensePlate;

    @NotBlank(message = "Le modèle du véhicule est obligatoire")
    private String model;

    @Positive(message = "La capacité doit être supérieure à zéro")
    @NotNull
    private BigDecimal capacity;

}
