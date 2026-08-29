package org.milkcenter.fleetservice.dto.request;

import lombok.*;

import org.milkcenter.fleetservice.enums.RouteStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStatusUpdateRequest {

    private RouteStatus status;
}
