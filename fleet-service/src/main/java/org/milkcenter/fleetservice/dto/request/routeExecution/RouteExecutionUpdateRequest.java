package org.milkcenter.fleetservice.dto.request.routeExecution;

import jakarta.validation.constraints.FutureOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.milkcenter.fleetservice.enums.RouteExecutionStatus;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteExecutionUpdateRequest {

    @FutureOrPresent(message = "La date doit être aujourd'hui ou dans le futur")
    private Date executionDate;

    private Long actualDriverId;

    private Long actualVehicleId;

}
