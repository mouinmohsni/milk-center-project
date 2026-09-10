package org.milkcenter.invoicingservice.client;

import org.milkcenter.invoicingservice.config.FeignClientConfig;
import org.milkcenter.invoicingservice.dto.response.client.FarmerClientResponse;
import org.milkcenter.invoicingservice.dto.response.client.MilkCollectionClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "collection-service",
        configuration = FeignClientConfig.class
)
public interface CollectionServiceClient {

    @GetMapping("/api/collections/farmer/{farmerId}/accepted")
    List<MilkCollectionClientResponse> getMonthlyAcceptedCollections(
            @PathVariable("farmerId") Long farmerId,
            @RequestParam("month") Integer month,
            @RequestParam("year") Integer year
    );

    @GetMapping("/api/farmers")
    List<FarmerClientResponse> getAllFarmers();
}

