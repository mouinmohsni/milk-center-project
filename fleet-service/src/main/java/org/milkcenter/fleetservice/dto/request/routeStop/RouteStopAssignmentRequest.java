package org.milkcenter.fleetservice.dto.request.routeStop;



import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopAssignmentRequest {

    // Une route est obligatoire pour cette opération.
    @NotNull(message = "L'identifiant de la route est obligatoire")
    private Long routeId;

    // Peut rester null : la route peut être choisie avant l'ordre.
    @Positive(message = "L'ordre doit être supérieur à zéro")
    private Integer sequenceOrder;

    private LocalTime plannedTime;
}

