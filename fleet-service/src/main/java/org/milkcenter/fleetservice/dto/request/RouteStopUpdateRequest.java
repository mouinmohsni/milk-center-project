package org.milkcenter.fleetservice.dto.request;


import lombok.*;
import jakarta.validation.constraints.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopUpdateRequest {

    private Long routeId;

    @NotNull(message = "L'ID du férmier est obligatoire")
    private  Long farmerId ;

    @Positive(message = "L'ordre de séquence doit être supérieur à zéro")
    private  Integer sequenceOrder ;

    private LocalTime plannedTime ;
}
