package org.milkcenter.invoicingservice.client;

import org.milkcenter.invoicingservice.config.FeignClientConfig;
import org.milkcenter.invoicingservice.dto.response.client.MonthlyMilkTotalClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "collection-service",
        configuration = FeignClientConfig.class
)
public interface CollectionServiceClient {

    @GetMapping("/api/collections/farmer/{farmerId}/monthly-total")
    MonthlyMilkTotalClientResponse getMonthlyMilkTotal(
            @PathVariable("farmerId") Long farmerId,
            @RequestParam("month") Integer month,
            @RequestParam("year") Integer year
    );
}
