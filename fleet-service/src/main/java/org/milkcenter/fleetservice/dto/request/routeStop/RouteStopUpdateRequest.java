package org.milkcenter.fleetservice.dto.request.routeStop;

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
public class RouteStopUpdateRequest {

    // Facultatif : null signifie qu'on ne change pas la route actuelle.
    private Long routeId;

    // Facultatif : permet de définir ou modifier l'ordre.
    @Positive(message = "L'ordre de séquence doit être supérieur à zéro")
    private Integer sequenceOrder;

    private LocalTime plannedTime;
}
