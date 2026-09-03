package org.milkcenter.fleetservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.fleetservice.dto.request.driver.DriverRequest;
import org.milkcenter.fleetservice.dto.request.driver.DriverStatusUpdateRequest;
import org.milkcenter.fleetservice.dto.request.driver.DriverUpdateRequest;
import org.milkcenter.fleetservice.dto.response.DriverResponse;
import org.milkcenter.fleetservice.enums.DriverStatus;
import org.milkcenter.fleetservice.model.Driver;
import org.milkcenter.fleetservice.repository.DriverRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    // =====================================================
    // READ
    // =====================================================

    public List<DriverResponse> getAllDrivers( ) {
        return driverRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DriverResponse getDriverById(Long id) {
        Driver driver = findDriverById(id);
        return mapToResponse(driver);
    }

    public DriverResponse getDriverByUserId(Long userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun chauffeur trouvé pour le userId : " + userId
                ));

        return mapToResponse(driver);
    }

    public DriverResponse getDriverByLicenseNumber(String licenseNumber) {
        Driver driver = driverRepository.findByLicenseNumber(licenseNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun chauffeur trouvé pour le numéro de permis : "
                                + licenseNumber
                ));

        return mapToResponse(driver);
    }

    public List<DriverResponse> getDriversByStatus(DriverStatus status) {
        return driverRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DriverResponse> getAvailableDrivers() {
        return getDriversByStatus(DriverStatus.AVAILABLE);
    }

    public List<DriverResponse> getDriversBySalary() {
        return driverRepository.findAllByOrderBySalaryDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // CREATE
    // =====================================================

    public DriverResponse createDriver(DriverRequest request) {

        if (driverRepository.existsByUserId(request.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un profil existe déjà pour l'utilisateur ID : "
                            + request.getUserId()
            );
        }

        if (driverRepository.findByLicenseNumber(
                request.getLicenseNumber()
        ).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le numéro de permis "
                            + request.getLicenseNumber()
                            + " est déjà utilisé"
            );
        }

        Driver driver = Driver.builder()
                .userId(request.getUserId())
                .licenseNumber(request.getLicenseNumber())
                .salary(request.getSalary())
                .status(
                        request.getStatus() != null
                                ? request.getStatus()
                                : DriverStatus.AVAILABLE
                )
                .build();

        Driver savedDriver = driverRepository.save(driver);
        return mapToResponse(savedDriver);
    }

    // =====================================================
    // UPDATE ADMINISTRATIF
    // =====================================================

    public DriverResponse updateDriver(
            Long id,
            DriverUpdateRequest request
    ) {
        Driver existingDriver = findDriverById(id);

        // Vérification de l'unicité du userId uniquement
        // si le userId est réellement modifié.
        if (!Objects.equals(
                existingDriver.getUserId(),
                request.getUserId()
        )) {
            if (driverRepository.existsByUserId(request.getUserId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Le userId " + request.getUserId()
                                + " est déjà utilisé par un autre chauffeur"
                );
            }
        }

        // Cette méthode exclut le chauffeur actuel grâce à son id.
        if (driverRepository.existsByLicenseNumberAndIdNot(
                request.getLicenseNumber(),
                id
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le numéro de permis "
                            + request.getLicenseNumber()
                            + " est déjà utilisé par un autre chauffeur"
            );
        }

        // Comme il s'agit d'un PUT, les trois champs doivent être
        // obligatoires dans DriverUpdateRequest.
        existingDriver.setUserId(request.getUserId());
        existingDriver.setLicenseNumber(request.getLicenseNumber());
        existingDriver.setSalary(request.getSalary());

        Driver updatedDriver = driverRepository.save(existingDriver);
        return mapToResponse(updatedDriver);
    }

    // =====================================================
    // UPDATE DU STATUT
    // =====================================================

    public DriverResponse updateStatusDriver(
            Long id,
            DriverStatusUpdateRequest request
    ) {
        Driver existingDriver = findDriverById(id);

        existingDriver.setStatus(request.getStatus());

        Driver updatedDriver = driverRepository.save(existingDriver);
        return mapToResponse(updatedDriver);
    }


    public void deleteDriver(Long id) {
        Driver existingDriver = findDriverById(id);
        driverRepository.delete(existingDriver);

    }

    // =====================================================
    // METHODES METIER
    // =====================================================

    public void validateDriverAvailability(Long driverId) {
        Driver driver = findDriverById(driverId);

        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le chauffeur avec l'ID " + driverId
                            + " n'est pas disponible. Statut actuel : "
                            + driver.getStatus()
            );
        }
    }

    public boolean isLicenseNumberAvailable(
            String licenseNumber,
            Long excludedDriverId
    ) {
        if (excludedDriverId == null) {
            return driverRepository.findByLicenseNumber(licenseNumber)
                    .isEmpty();
        }

        return !driverRepository.existsByLicenseNumberAndIdNot(
                licenseNumber,
                excludedDriverId
        );
    }

    // =====================================================
    // METHODES PRIVEES
    // =====================================================

    private Driver findDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun chauffeur trouvé avec l'ID : " + id
                ));
    }

    private DriverResponse mapToResponse(Driver driver) {
        return DriverResponse.builder()
                .id(driver.getId())
                .userId(driver.getUserId())
                .licenseNumber(driver.getLicenseNumber())
                .salary(driver.getSalary())
                .status(driver.getStatus())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
