package org.milkcenter.fleetservice.dto.response;

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
public class RouteExecutionResponse {

    private Long id;

    private Long routeId;

    private Long actualDriverId;

    private Long actualVehicleId;

    private Date executionDate;

    private RouteExecutionStatus status;

    private Date startedAt;

    private Date finishedAt;

    private Date createdAt;

    private Date updatedAt;
}
