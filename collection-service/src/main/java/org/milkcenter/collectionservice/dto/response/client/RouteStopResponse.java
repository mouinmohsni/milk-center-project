package org.milkcenter.collectionservice.dto.response.client;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopResponse {

    private Long id ;
    private Long routeId;
    private  Long farmerId ;

}
