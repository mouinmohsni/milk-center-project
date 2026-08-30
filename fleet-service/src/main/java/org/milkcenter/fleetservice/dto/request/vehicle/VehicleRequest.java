package org.milkcenter.fleetservice.dto.request.vehicle;

import org.milkcenter.fleetservice.enums.VehicleStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;




@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequest {

    @NotBlank(message = "La plaque d'immatriculation est obligatoire")
    private String licensePlate;

    @NotBlank(message = "Le modèle du véhicule est obligatoire")
    private String model;

    @NotNull(message = "La capacité est obligatoire")
    @Positive(message = "La capacité doit être supérieure à zéro")
    private BigDecimal capacity;

    @NotNull(message = "Le kilométrage est obligatoire")
    @PositiveOrZero(message = "Le kilométrage ne peut pas être négatif")
    private Long km;

    @PositiveOrZero(message = "Le kilométrage de la dernière vidange ne peut pas être négatif")
    private Long lastOilChangeMileage;

    private VehicleStatus status;

}
