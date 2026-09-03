package org.milkcenter.fleetservice.dto.request.fuel;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelConsumptionRequest {
    @NotNull(message = "L'ID du véhicule est obligatoire")
    private Long vehicleId;

    private List<Long> routeExecutionIds; // La liste des exécutions liées à ce plein

    @NotBlank(message = "Le type de carburant est obligatoire")
    private String fuelType;

    @NotNull(message = "La date est obligatoire")
    private Date fuelDate;

    @NotNull(message = "Le kilométrage est obligatoire")
    @Min(0)
    private Long odometer;

    @NotNull(message = "La quantité en litres est obligatoire")
    @DecimalMin("0.01")
    private BigDecimal liters;

    @NotNull(message = "Le prix au litre est obligatoire")
    @DecimalMin("0.001")
    private BigDecimal pricePerLiter;
}
