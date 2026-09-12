package org.milkcenter.invoicingservice.service;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.milkcenter.invoicingservice.client.CollectionServiceClient;
import org.milkcenter.invoicingservice.dto.response.client.FarmerClientResponse;
import org.milkcenter.invoicingservice.dto.response.client.MilkCollectionClientResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionServiceResilientClient {

    private final CollectionServiceClient collectionServiceClient;

    @CircuitBreaker(
            name = "collectionservice",
            fallbackMethod = "getMonthlyAcceptedCollectionsFallback"
    )
    public List<MilkCollectionClientResponse> getMonthlyAcceptedCollections(
            Long farmerId,
            Integer month,
            Integer year
    ) {
        List<MilkCollectionClientResponse> response =
                collectionServiceClient.getMonthlyAcceptedCollections(
                        farmerId,
                        month,
                        year
                );

        return response == null ? List.of() : response;
    }

    public List<MilkCollectionClientResponse> getMonthlyAcceptedCollectionsFallback(
            Long farmerId,
            Integer month,
            Integer year,
            Throwable cause
    ) {
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

    @CircuitBreaker(
            name = "collectionservice",
            fallbackMethod = "getAllFarmersFallback"
    )
    public List<FarmerClientResponse> getAllFarmers() {
        List<FarmerClientResponse> response =
                collectionServiceClient.getAllFarmers();

        System.out.println("getAllFarmers========="+response);

        return response == null ? List.of() : response;
    }

    public List<FarmerClientResponse> getAllFarmersFallback(
            Throwable cause
    ) {
        if (cause instanceof FeignException feignException
                && feignException.status() == 404) {
            return List.of();
        }

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Impossible de récupérer la liste des farmers",
                cause
        );
    }
}

