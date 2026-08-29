package org.milkcenter.fleetservice.dto.request;


import lombok.*;
import jakarta.validation.constraints.*;
import org.milkcenter.fleetservice.enums.DriverStatus;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverStatusUpdateRequest {

    @NotNull(message = "Le nouveau statut est obligatoire")
    private DriverStatus status = DriverStatus.AVAILABLE ;
}
