package org.milkcenter.fleetservice.dto.response;


import lombok.*;

import org.milkcenter.fleetservice.enums.AssignmentStatusRouteStop;

import java.time.LocalTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopResponse {

    private Long id ;
    private Long routeId;
    private  Long farmerId ;
    private  Integer sequenceOrder ;
    private LocalTime plannedTime ;
    private AssignmentStatusRouteStop assignmentStatus ;
    private Date createdAt ;
    private Date updatedAt ;

}
