package org.milkcenter.fleetservice.dto.response;


import lombok.*;
import org.milkcenter.fleetservice.enums.VehicleStatus;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {


    private Long id ;
    private String licensePlate ;
    private String model ;
    private BigDecimal capacity ;
    private Long km ;
    private Long lastOilChangeMileage  ;
    private VehicleStatus status ;
    private Date createdAt ;
    private Date updatedAt ;
}
