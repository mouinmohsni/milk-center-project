package org.milkcenter.fleetservice.dto.request;


import jakarta.validation.constraints.*;
import lombok.*;


import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopRequest {

    private Long routeId;

    @NotNull(message = "L'ID du férmier est obligatoire")
    private  Long farmerId ;

    @Positive
    private  Integer sequenceOrder ;

    private LocalTime plannedTime ;

}
