package org.milkcenter.fleetservice.dto.request.maintenance;

import jakarta.validation.constraints.*;
import lombok.*;
import org.milkcenter.fleetservice.enums.MaintenanceType;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecordRequest {
    @NotNull(message = "L'ID du véhicule est obligatoire")
    private Long vehicleId;

    @NotNull(message = "Le type de maintenance est obligatoire")
    private MaintenanceType maintenanceType;

    @Size(max = 500)
    private String description;

    @NotNull(message = "La date est obligatoire")
    private Date maintenanceDate;

    @Min(0)
    private Long odometer;

    @DecimalMin("0.0")
    private BigDecimal cost;

    private String provider;
    private Long nextMaintenanceOdometer;
}
