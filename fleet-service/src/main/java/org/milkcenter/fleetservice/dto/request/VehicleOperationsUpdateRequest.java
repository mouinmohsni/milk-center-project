package org.milkcenter.fleetservice.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleOperationsUpdateRequest {


    @PositiveOrZero(message = "Le kilométrage ne peut pas être négatif")
    private Long km;

    @PositiveOrZero(message = "Le kilométrage de la dernière vidange ne peut pas être négatif")
    private Long lastOilChangeMileage;
}
