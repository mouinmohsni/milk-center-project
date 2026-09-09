package org.milkcenter.invoicingservice.service;



import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

import org.milkcenter.invoicingservice.client.CollectionServiceClient;
import org.milkcenter.invoicingservice.dto.response.client.MonthlyMilkTotalClientResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CollectionServiceResilientClient {

    private final CollectionServiceClient collectionServiceClient ;

    @CircuitBreaker(
            name = "collectionservice",
            fallbackMethod = "getMonthlyMilkTotalFallback"
    )
    public MonthlyMilkTotalClientResponse getMonthlyMilkTotal(
            Long farmerId,
            Integer month,
            Integer year
    ){
        MonthlyMilkTotalClientResponse response = collectionServiceClient.getMonthlyMilkTotal(farmerId,month,year);
        if (response == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Le total mensuel de lait est introuvable"
            );
        }

        return response;
    }

    public MonthlyMilkTotalClientResponse getMonthlyMilkTotalFallback(
            Long farmerId,
            Integer month,
            Integer year,
            Throwable cause
    ){
        if (cause instanceof FeignException feignException
                && feignException.status() == 404) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Aucune collecte ACCEPTED trouvée pour cette période"
            );
        }

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Le service collection-service est temporairement indisponible",
                cause
        );
    }
}
