package org.milkcenter.fleetservice.dto.request.route;

import lombok.*;

import org.milkcenter.fleetservice.enums.RouteStatus;
import jakarta.validation.constraints.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStatusUpdateRequest {

    @NotNull(message = "Le statut de la route est obligatoire")
    private RouteStatus status;

}
