package org.milkcenter.fleetservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.fuel.FuelConsumptionRequest;
import org.milkcenter.fleetservice.dto.request.fuel.FuelConsumptionUpdateRequest;
import org.milkcenter.fleetservice.dto.response.FuelConsumptionResponse;
import org.milkcenter.fleetservice.model.FuelConsumption;
import org.milkcenter.fleetservice.model.RouteExecution;
import org.milkcenter.fleetservice.model.Vehicle;
import org.milkcenter.fleetservice.repository.FuelConsumptionRepository;
import org.milkcenter.fleetservice.repository.RouteExecutionRepository;
import org.milkcenter.fleetservice.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class FuelConsumptionService {

    private final FuelConsumptionRepository fuelConsumptionRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteExecutionRepository routeExecutionRepository;




    private List<RouteExecution> loadAndValidateExecutions(
            Long vehicleId,
            List<Long> executionIds
    ) {
        if (executionIds == null || executionIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> distinctIds = executionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctIds.size() != executionIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La liste des exécutions contient des identifiants invalides ou dupliqués"
            );
        }

        List<RouteExecution> executions =
                routeExecutionRepository.findAllById(distinctIds);

        Set<Long> foundIds = executions.stream()
                .map(RouteExecution::getId)
                .collect(Collectors.toSet());

        if (foundIds.size() != distinctIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Une ou plusieurs exécutions de route n'existent pas"
            );
        }

        boolean belongsToAnotherVehicle = executions.stream()
                .anyMatch(execution ->
                        execution.getActualVehicle() == null
                                || !vehicleId.equals(
                                execution.getActualVehicle().getId()
                        )
                );

        if (belongsToAnotherVehicle) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une ou plusieurs exécutions n'appartiennent pas au véhicule sélectionné"
            );
        }

        return executions;
    }


    @Transactional
    public FuelConsumptionResponse createFuelConsumption(FuelConsumptionRequest request ) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule non trouvé"));


        List<RouteExecution> executions =
                loadAndValidateExecutions(
                        vehicle.getId(),
                        request.getRouteExecutionIds()
                );




        FuelConsumption fuel = FuelConsumption.builder()
                .vehicle(vehicle)
                .routeExecutions(executions)
                .fuelType(request.getFuelType())
                .fuelDate(request.getFuelDate())
                .odometer(request.getOdometer())
                .liters(request.getLiters())
                .pricePerLiter(request.getPricePerLiter())
                .totalCost(request.getLiters().multiply(request.getPricePerLiter()))
                .build();

        // On met à jour le kilométrage du véhicule si le plein est plus récent
        if (request.getOdometer() > vehicle.getKm()) {
            vehicle.setKm(request.getOdometer());
            vehicleRepository.save(vehicle);
        }

        return mapToResponse(fuelConsumptionRepository.save(fuel));
    }

    @Transactional
    public FuelConsumptionResponse updateFuelConsumption(Long id, FuelConsumptionUpdateRequest request) {
        FuelConsumption fuel = fuelConsumptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enregistrement non trouvé"));

        if (request.getRouteExecutionIds() != null) {
            List<RouteExecution> executions =
                    loadAndValidateExecutions(
                            fuel.getVehicle().getId(),
                            request.getRouteExecutionIds()
                    );

            fuel.setRouteExecutions(executions);
        }

        if (request.getFuelType() != null) fuel.setFuelType(request.getFuelType());
        if (request.getFuelDate() != null) fuel.setFuelDate(request.getFuelDate());
        if (request.getOdometer() != null) fuel.setOdometer(request.getOdometer());
        if (request.getLiters() != null) fuel.setLiters(request.getLiters());
        if (request.getPricePerLiter() != null) fuel.setPricePerLiter(request.getPricePerLiter());

        // Recalcul du total
        if (fuel.getLiters() != null && fuel.getPricePerLiter() != null) {
            fuel.setTotalCost(fuel.getLiters().multiply(fuel.getPricePerLiter()));
        }

        return mapToResponse(fuelConsumptionRepository.save(fuel));
    }

    public List<FuelConsumptionResponse> getFuelByVehicle(Long vehicleId) {
        return fuelConsumptionRepository.findByVehicleIdOrderByFuelDateDesc(vehicleId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFuel(Long id) {
        fuelConsumptionRepository.deleteById(id);
    }

    private FuelConsumptionResponse mapToResponse(FuelConsumption fuel) {
        List<Long> executionIds = null;
        if (fuel.getRouteExecutions() != null) {
            executionIds = fuel.getRouteExecutions().stream()
                    .map(RouteExecution::getId)
                    .collect(Collectors.toList());
        }

        return FuelConsumptionResponse.builder()
                .id(fuel.getId())
                .vehicleId(fuel.getVehicle().getId())
                .licensePlate(fuel.getVehicle().getLicensePlate())
                .routeExecutionIds(executionIds)
                .fuelType(fuel.getFuelType())
                .fuelDate(fuel.getFuelDate())
                .odometer(fuel.getOdometer())
                .liters(fuel.getLiters())
                .pricePerLiter(fuel.getPricePerLiter())
                .totalCost(fuel.getTotalCost())
                .createdAt(fuel.getCreatedAt())
                .build();
    }
}
