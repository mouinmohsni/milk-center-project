package org.milkcenter.fleetservice.dto.request.fuel;

import lombok.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelConsumptionUpdateRequest {
    private List<Long> routeExecutionIds;
    private String fuelType;
    private Date fuelDate;
    private Long odometer;
    private BigDecimal liters;
    private BigDecimal pricePerLiter;
}
