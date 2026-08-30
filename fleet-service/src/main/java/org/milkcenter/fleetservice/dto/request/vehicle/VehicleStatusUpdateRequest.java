package org.milkcenter.fleetservice.dto.request.vehicle;

import lombok.*;
import jakarta.validation.constraints.*;
import org.milkcenter.fleetservice.enums.VehicleStatus;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleStatusUpdateRequest {

    @NotNull(message = "Le nouveau statut est obligatoire")
    private VehicleStatus status;
}
