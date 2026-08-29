package org.milkcenter.fleetservice.dto.response;

import lombok.*;
import org.milkcenter.fleetservice.enums.DriverStatus;

import java.math.BigDecimal;
import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponse {

    private Long id ;
    private Long userId ;
    private String licenseNumber ;
    private BigDecimal salary ;
    private DriverStatus status ;
    private Date createdAt ;
    private Date updatedAt ;
}
