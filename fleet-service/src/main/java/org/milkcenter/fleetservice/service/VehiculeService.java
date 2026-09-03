package org.milkcenter.fleetservice.service;


import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.vehicle.VehicleManagerUpdateRequest;
import org.milkcenter.fleetservice.dto.request.vehicle.VehicleOperationsUpdateRequest;
import org.milkcenter.fleetservice.dto.request.vehicle.VehicleRequest;
import org.milkcenter.fleetservice.dto.request.vehicle.VehicleStatusUpdateRequest;
import org.milkcenter.fleetservice.dto.response.VehicleResponse;
import org.milkcenter.fleetservice.enums.RouteExecutionStatus;
import org.milkcenter.fleetservice.enums.VehicleStatus;
import org.milkcenter.fleetservice.model.Driver;
import org.milkcenter.fleetservice.model.Route;
import org.milkcenter.fleetservice.model.RouteExecution;
import org.milkcenter.fleetservice.model.Vehicle;
import org.milkcenter.fleetservice.repository.RouteExecutionRepository;
import org.milkcenter.fleetservice.repository.RouteRepository;
import org.milkcenter.fleetservice.repository.VehicleRepository;
import org.milkcenter.fleetservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehiculeService {

    private final VehicleRepository vehicleRepository ;
    private final CurrentUserService currentUserService;
    private  final RouteExecutionRepository routeExecutionRepository;

    private static final Long OIL_CHANGE_INTERVAL_KM = 10_000L;


    public List<VehicleResponse> getAllVehiculs(){
        return vehicleRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public VehicleResponse getVehiculById(Long id){

        Vehicle vehicle = findVehicleById(id);

        return mapToResponse(vehicle);

    }

    public VehicleResponse getVehiculBylicensePlate(String licensePlate){

        Vehicle vehicle = vehicleRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "le véhicule non trouvé avec  license Plate: " + licensePlate
                ));

        return mapToResponse(vehicle);

    }

    public List<VehicleResponse> getVehiclesByStatus(VehicleStatus status){

        return vehicleRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VehicleResponse> getVehiclesByModel(String model){

        return vehicleRepository.findByModel(model)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    public VehicleResponse createVehicul(VehicleRequest request){

        if(vehicleRepository.existsByLicensePlate(request.getLicensePlate())){

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une véhicule  existe déjà avec cette immatriculation : " + request.getLicensePlate());
        }


        Vehicle vehicle = Vehicle.builder()

                .licensePlate(request.getLicensePlate())
                .model(request.getModel())
                .capacity(request.getCapacity())
                .km(request.getKm())
                .lastOilChangeMileage(request.getLastOilChangeMileage())
                .status(
                        request.getStatus() != null
                                ? request.getStatus()
                                : VehicleStatus.READY
                )

                .build();

         Vehicle saved = vehicleRepository.save(vehicle);

         return mapToResponse(saved);

    }

     public VehicleResponse updateVehicleByManager(Long id, VehicleManagerUpdateRequest request){

         Vehicle vehicle = findVehicleById(id);


         if (!isLicensePlateAvailable(
                 request.getLicensePlate(),
                 id
         )) {
             throw new ResponseStatusException(
                     HttpStatus.CONFLICT,
                     "Cette plaque d'immatriculation est déjà utilisée"
             );
         }



         vehicle.setLicensePlate(request.getLicensePlate());
         vehicle.setModel(request.getModel());
         vehicle.setCapacity(request.getCapacity());


         Vehicle updatedVehicle  = vehicleRepository.save(vehicle);
         return mapToResponse(updatedVehicle);
     }


    public VehicleResponse updateVehicleStatus(Long vehicleId, VehicleStatusUpdateRequest request){
        Vehicle vehicle = findVehicleById(vehicleId);

        vehicle.setStatus(request.getStatus());

        Vehicle updatedVehicle  = vehicleRepository.save(vehicle);
        return mapToResponse(updatedVehicle);
    }
//==============================================================
    public VehicleResponse updateVehicleByOperator(
            Long vehicleId,
            VehicleOperationsUpdateRequest request
    ) {
        Vehicle vehicle = findVehicleById(vehicleId);


        checkDriverAccess(vehicleId);

        if (request.getKm() == null
                && request.getLastOilChangeMileage() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Au moins une valeur doit être fournie"
            );
        }

        Long newKm = request.getKm() != null
                ? request.getKm()
                : vehicle.getKm();

        Long newLastOilChangeMileage =
                request.getLastOilChangeMileage() != null
                        ? request.getLastOilChangeMileage()
                        : vehicle.getLastOilChangeMileage();

        if (vehicle.getKm() != null
                && newKm != null
                && newKm < vehicle.getKm()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nouveau kilométrage ne peut pas être inférieur "
                            + "au kilométrage actuel"
            );
        }

        if (newKm != null
                && newLastOilChangeMileage != null
                && newLastOilChangeMileage > newKm) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le kilométrage de la dernière vidange ne peut pas "
                            + "dépasser le kilométrage actuel"
            );
        }

        if (request.getKm() != null) {
            vehicle.setKm(request.getKm());
        }

        if (request.getLastOilChangeMileage() != null) {
            vehicle.setLastOilChangeMileage(
                    request.getLastOilChangeMileage()
            );
        }
        if (isOilChangeDue(vehicle)) {
            vehicle.setStatus(VehicleStatus.NEED_MAINTENANCE);
        }


        Vehicle updatedVehicle = vehicleRepository.save(vehicle);


        return mapToResponse(updatedVehicle);
    }

    public void validateVehicleAvailability(Long vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);

        if (vehicle.getStatus() != VehicleStatus.READY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le véhicule avec l'ID " + vehicleId
                            + " n'est pas disponible. Statut actuel : "
                            + vehicle.getStatus()
            );
        }
    }

    public boolean isLicensePlateAvailable(
            String licensePlate,
            Long excludedVehicleId
    ) {
        if (excludedVehicleId == null) {
            return !vehicleRepository.existsByLicensePlate(licensePlate);
        }

        return !vehicleRepository.existsByLicensePlateAndIdNot(
                licensePlate,
                excludedVehicleId
        );
    }





    public void deleteVehicle(Long id){

        Vehicle vehicle = findVehicleById(id);

        vehicleRepository.delete(vehicle);
    }



    public boolean isOilChangeDue(Vehicle vehicle) {


        Long currentKm = vehicle.getKm();
        Long lastOilChangeMileage = vehicle.getLastOilChangeMileage();


        if (currentKm == null || lastOilChangeMileage == null) {
            return true;
        }

        Long kilometersSinceLastOilChange =
                currentKm - lastOilChangeMileage;

        return kilometersSinceLastOilChange >= OIL_CHANGE_INTERVAL_KM;
    }











    private Vehicle findVehicleById(Long id){
        return vehicleRepository.findById(id)
                 .orElseThrow(() -> new ResponseStatusException(
                         HttpStatus.NOT_FOUND,
                         "Aucun véhicule trouvé avec l'ID : " + id

                 ));
     }

    // ============================================
    // Mapper privé — Entité → DTO Response
    // ============================================


    private void checkDriverAccess(Long vehicleId) {
        String role = currentUserService.getCurrentRole();

        if ("MANAGER".equals(role)) {
            return;
        }
        if ("DRIVER".equals(role)) {
            Long currentUserId = currentUserService.getCurrentUserId();

            Optional<RouteExecution> routeExecution =
                    routeExecutionRepository
                            .findByActualDriver_UserIdAndActualVehicle_IdAndStatus(
                                    currentUserId,
                                    vehicleId,
                                    RouteExecutionStatus.ACTIVE
                            );

            if (routeExecution.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Ce véhicule ne vous est pas affecté"
                );
            }
            return;
        }
        // Refuser également les rôles inconnus ou absents.
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Rôle non autorisé pour cette opération"
        );
    }


    private VehicleResponse mapToResponse(Vehicle vehicle ){
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .model(vehicle.getModel())
                .capacity(vehicle.getCapacity())
                .status(vehicle.getStatus())
                .km(vehicle.getKm())
                .lastOilChangeMileage(vehicle.getLastOilChangeMileage())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}
