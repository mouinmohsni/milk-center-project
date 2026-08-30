package org.milkcenter.fleetservice.dto.request.route;



import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.milkcenter.fleetservice.dto.request.routeStop.RouteStopRequest;
import org.milkcenter.fleetservice.enums.RouteStatus;



import java.util.*;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotNull(message = "L'ID du chauffeur est obligatoire")
    private Long driverId; // 👈 Utilisez Long, pas l'objet Driver

    @NotNull(message = "L'ID du véhicule est obligatoire")
    private Long vehicleId; // 👈 Utilisez Long, pas l'objet Vehicle

    @FutureOrPresent(message = "La date doit être aujourd'hui ou dans le futur")
    private Date plannedDate;

    private RouteStatus status;

    @Valid
    private List<RouteStopRequest> stops;
}

