package org.milkcenter.collectionservice.client;

import org.milkcenter.collectionservice.config.FeignClientConfig;

import org.milkcenter.collectionservice.dto.response.client.RouteStopResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "fleet-service",
        configuration = FeignClientConfig.class,
        path = "/api/route-stops"

)
public interface RouteStopClient {

    @GetMapping("/{id}")
    RouteStopResponse getRouteStopById(@PathVariable("id") Long id);
}
