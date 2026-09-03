package org.milkcenter.fleetservice.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelConsumptionResponse {
    private Long id;
    private Long vehicleId;
    private String licensePlate;
    private List<Long> routeExecutionIds;
    private String fuelType;
    private Date fuelDate;
    private Long odometer;
    private BigDecimal liters;
    private BigDecimal pricePerLiter;
    private BigDecimal totalCost;
    private Date createdAt;
}
