package org.milkcenter.fleetservice.dto.response;


import lombok.*;
import org.milkcenter.fleetservice.enums.RouteStatus;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponse {


    private Long id ;
    private String name ;
    private Long driverId;
    private Long vehicleId;
    private Date plannedDate ;
    private RouteStatus status ;
    private List<Long> stops ;
    private Date createdAt ;
    private Date updatedAt ;

}
