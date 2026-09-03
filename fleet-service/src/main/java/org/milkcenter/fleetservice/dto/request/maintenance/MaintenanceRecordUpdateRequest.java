package org.milkcenter.fleetservice.dto.request.maintenance;

import lombok.*;
import org.milkcenter.fleetservice.enums.MaintenanceStatus;
import org.milkcenter.fleetservice.enums.MaintenanceType;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecordUpdateRequest {
    private MaintenanceType maintenanceType;
    private MaintenanceStatus status;
    private String description;
    private Date maintenanceDate;
    private Long odometer;
    private BigDecimal cost;
    private String provider;
    private Long nextMaintenanceOdometer;
}
