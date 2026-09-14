package org.milkcenter.collectionservice.service;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.milkcenter.collectionservice.client.RouteStopClient;
import org.milkcenter.collectionservice.dto.response.client.RouteStopResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RouteStopResilientClient {

    private final RouteStopClient routeStopClient;

    private static final Logger log =
            LoggerFactory.getLogger(RouteStopResilientClient.class);

    /**
     * Appelle fleet-service avec protection Circuit Breaker.
     */
    @CircuitBreaker(
            name = "fleetService",
            fallbackMethod = "getRouteStopByIdFallback"
    )
    public RouteStopResponse getRouteStopById(Long routeStopId) {
        RouteStopResponse response = routeStopClient.getRouteStopById(routeStopId);

        if (response == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Route stop introuvable"
            );
        }

        return response;
    }

    /**
     * Signature obligatoire : mêmes paramètres que la méthode originale,
     * puis une exception en dernier paramètre.
     */
    public RouteStopResponse getRouteStopByIdFallback(
            Long routeStopId,
            Throwable cause
    ) {

        log.error(
                "Echec appel fleet-service pour routeStopId={}, type={}, message={}",
                routeStopId,
                cause.getClass().getName(),
                cause.getMessage(),
                cause
        );
        // Si fleet-service répond réellement 404, on conserve le 404.
        if (cause instanceof FeignException feignException
                && feignException.status() == 404) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Route stop introuvable"
            );
        }

        // Panne réseau, timeout, service arrêté ou circuit ouvert.
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Le service fleet-service est temporairement indisponible. " +
                        "La vérification du route stop ne peut pas être effectuée.",
                cause
        );
    }
}
