package org.milkcenter.fleetservice.dto.response;

import lombok.*;
import org.milkcenter.fleetservice.enums.MaintenanceStatus;
import org.milkcenter.fleetservice.enums.MaintenanceType;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecordResponse {
    private Long id;
    private Long vehicleId;
    private String licensePlate;
    private MaintenanceType maintenanceType;
    private MaintenanceStatus status;
    private String description;
    private Date maintenanceDate;
    private Long odometer;
    private BigDecimal cost;
    private String provider;
    private Long nextMaintenanceOdometer;
    private Date createdAt;
}
