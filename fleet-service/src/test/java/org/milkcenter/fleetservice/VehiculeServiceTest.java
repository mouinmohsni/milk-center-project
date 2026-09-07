package org.milkcenter.fleetservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.milkcenter.fleetservice.dto.request.vehicle.VehicleOperationsUpdateRequest;
import org.milkcenter.fleetservice.enums.VehicleStatus;
import org.milkcenter.fleetservice.model.Vehicle;
import org.milkcenter.fleetservice.repository.RouteExecutionRepository;
import org.milkcenter.fleetservice.repository.VehicleRepository;
import org.milkcenter.fleetservice.security.CurrentUserService;
import org.milkcenter.fleetservice.service.VehiculeService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculeServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RouteExecutionRepository routeExecutionRepository;

    @InjectMocks
    private VehiculeService vehiculeService;

    @Test
    void oilChangeIsDueWhenIntervalReachesTenThousandKilometers() {
        Vehicle vehicle = Vehicle.builder()
                .km(110_000L)
                .lastOilChangeMileage(100_000L)
                .status(VehicleStatus.READY)
                .build();

        assertTrue(vehiculeService.isOilChangeDue(vehicle));
    }

    @Test
    void oilChangeIsNotDueBeforeInterval() {
        Vehicle vehicle = Vehicle.builder()
                .km(109_999L)
                .lastOilChangeMileage(100_000L)
                .status(VehicleStatus.READY)
                .build();

        assertFalse(vehiculeService.isOilChangeDue(vehicle));
    }

    @Test
    void operatorCannotDecreaseCurrentMileage() {
        Vehicle vehicle = Vehicle.builder()
                .id(1L)
                .licensePlate("123-TUN-456")
                .model("Truck")
                .capacity(new BigDecimal("1000"))
                .km(50_000L)
                .lastOilChangeMileage(45_000L)
                .status(VehicleStatus.READY)
                .build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(currentUserService.getCurrentRole()).thenReturn("MANAGER");

        VehicleOperationsUpdateRequest request =
                VehicleOperationsUpdateRequest.builder()
                        .km(49_000L)
                        .build();

        assertThrows(ResponseStatusException.class,
                () -> vehiculeService.updateVehicleByOperator(1L, request));
    }

    @Test
    void operatorUpdateMarksVehicleForMaintenanceWhenOilChangeIsDue() {
        Vehicle vehicle = Vehicle.builder()
                .id(1L)
                .licensePlate("123-TUN-456")
                .model("Truck")
                .capacity(new BigDecimal("1000"))
                .km(50_000L)
                .lastOilChangeMileage(40_000L)
                .status(VehicleStatus.READY)
                .build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(currentUserService.getCurrentRole()).thenReturn("MANAGER");
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VehicleOperationsUpdateRequest request =
                VehicleOperationsUpdateRequest.builder()
                        .km(50_001L)
                        .build();

        var response = vehiculeService.updateVehicleByOperator(1L, request);

        assertEquals(VehicleStatus.NEED_MAINTENANCE, response.getStatus());
    }
}
