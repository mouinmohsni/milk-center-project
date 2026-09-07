package org.milkcenter.fleetservice.dto.request.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceCompleteRequest {

    private BigDecimal cost;
    private Long odometer;
    private Long nextMaintenanceOdometer;
}
