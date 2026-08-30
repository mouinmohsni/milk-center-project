package org.milkcenter.fleetservice.dto.request.routeExecution;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteExecutionRequest {

    @NotNull(message = "L'identifiant de la route est obligatoire")
    private Long routeId;

    @NotNull(message = "La date d'exécution est obligatoire")
    @FutureOrPresent(message = "La date d'exécution doit être aujourd'hui ou dans le futur")
    private Date executionDate;

    // Facultatifs : si absents, le Service utilise les affectations de Route.
    private Long actualDriverId;

    private Long actualVehicleId;
}
