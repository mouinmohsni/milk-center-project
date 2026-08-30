package org.milkcenter.fleetservice.dto.request.routeExecution;



import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.milkcenter.fleetservice.enums.RouteExecutionStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteExecutionStatusUpdateRequest {

    @NotNull(message = "Le statut de l'exécution est obligatoire")
    private RouteExecutionStatus status;
}
