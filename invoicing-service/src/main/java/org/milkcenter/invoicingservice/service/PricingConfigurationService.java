package org.milkcenter.invoicingservice.service;


import lombok.RequiredArgsConstructor;
import org.milkcenter.invoicingservice.dto.request.PricingConfigurationCreateRequest;
import org.milkcenter.invoicingservice.dto.request.PricingConfigurationPatchRequest;
import org.milkcenter.invoicingservice.dto.response.PricingConfigurationResponse;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;
import org.milkcenter.invoicingservice.model.PricingConfiguration;
import org.milkcenter.invoicingservice.repository.PricingConfigurationRepository;
import org.milkcenter.invoicingservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PricingConfigurationService {

    private static final int PRICE_SCALE = 3;
    private static final int TAX_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final PricingConfigurationRepository pricingConfigurationRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public PricingConfigurationResponse create(
            PricingConfigurationCreateRequest request
    ) {
        requireManager();

        if (request == null) {
            throw badRequest("Les données de configuration sont obligatoires");
        }

        validateBusinessRules(
                request.getInvoiceType(),
                request.getProductName(),
                request.getSaleUnit(),
                request.getPackageWeightKg(),
                request.getUnitPrice(),
                request.getTaxRate(),
                request.getEffectiveFrom()
        );

        boolean exists =
                pricingConfigurationRepository
                        .existsByInvoiceTypeAndProductNameAndSaleUnitAndPackageWeightKgAndEffectiveFromAndDeletedFalse(
                                request.getInvoiceType(),
                                normalizeText(request.getProductName()),
                                request.getSaleUnit(),
                                normalizePackageWeight(
                                        request.getSaleUnit(),
                                        request.getPackageWeightKg()
                                ),
                                request.getEffectiveFrom()
                        );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une configuration identique existe déjà pour cette date"
            );
        }

        PricingConfiguration configuration = PricingConfiguration.builder()
                .invoiceType(request.getInvoiceType())
                .productName(normalizeText(request.getProductName()))
                .saleUnit(request.getSaleUnit())
                .packageWeightKg(
                        normalizePackageWeight(
                                request.getSaleUnit(),
                                request.getPackageWeightKg()
                        )
                )
                .unitPrice(scalePrice(request.getUnitPrice()))
                .taxRate(scaleTax(request.getTaxRate()))
                .effectiveFrom(request.getEffectiveFrom())
                .active(request.getActive() == null || request.getActive())
                .deleted(false)
                .build();

        return mapToResponse(
                pricingConfigurationRepository.save(configuration)
        );
    }

    @Transactional(readOnly = true)
    public List<PricingConfigurationResponse> getAll() {
        requireManager();

        return pricingConfigurationRepository
                .findByDeletedFalseOrderByEffectiveFromDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PricingConfigurationResponse getById(Long id) {
        requireManager();
        return mapToResponse(findActiveConfigurationById(id));
    }

    @Transactional(readOnly = true)
    public List<PricingConfigurationResponse> getByInvoiceType(
            InvoiceType invoiceType
    ) {
        requireManager();

        if (invoiceType == null) {
            throw badRequest("Le type de facture est obligatoire");
        }

        return pricingConfigurationRepository
                .findByInvoiceTypeAndDeletedFalseOrderByProductNameAscEffectiveFromDesc(
                        invoiceType
                )
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PricingConfigurationResponse patch(
            Long id,
            PricingConfigurationPatchRequest request
    ) {
        requireManager();

        if (request == null) {
            throw badRequest("Les données de modification sont obligatoires");
        }

        PricingConfiguration configuration = findActiveConfigurationById(id);

        if (request.getInvoiceType() != null) {
            configuration.setInvoiceType(request.getInvoiceType());
        }

        if (request.getProductName() != null) {
            configuration.setProductName(
                    normalizeText(request.getProductName())
            );
        }

        if (request.getSaleUnit() != null) {
            configuration.setSaleUnit(request.getSaleUnit());
        }

        if (request.getPackageWeightKg() != null) {
            configuration.setPackageWeightKg(
                    request.getPackageWeightKg()
            );
        }

        if (request.getUnitPrice() != null) {
            configuration.setUnitPrice(
                    scalePrice(request.getUnitPrice())
            );
        }

        if (request.getTaxRate() != null) {
            configuration.setTaxRate(
                    scaleTax(request.getTaxRate())
            );
        }

        if (request.getEffectiveFrom() != null) {
            configuration.setEffectiveFrom(
                    request.getEffectiveFrom()
            );
        }

        if (request.getActive() != null) {
            configuration.setActive(request.getActive());
        }

        validateBusinessRules(
                configuration.getInvoiceType(),
                configuration.getProductName(),
                configuration.getSaleUnit(),
                configuration.getPackageWeightKg(),
                configuration.getUnitPrice(),
                configuration.getTaxRate(),
                configuration.getEffectiveFrom()
        );

        configuration.setPackageWeightKg(
                normalizePackageWeight(
                        configuration.getSaleUnit(),
                        configuration.getPackageWeightKg()
                )
        );

        return mapToResponse(
                pricingConfigurationRepository.save(configuration)
        );
    }

    @Transactional
    public PricingConfigurationResponse softDelete(Long id) {
        requireManager();

        PricingConfiguration configuration =
                findActiveConfigurationById(id);

        configuration.setActive(false);
        configuration.setDeleted(true);
        configuration.setDeletedAt(LocalDateTime.now());

        return mapToResponse(
                pricingConfigurationRepository.save(configuration)
        );
    }

    @Transactional
    public void hardDelete(Long id) {
        requireManager();

        PricingConfiguration configuration =
                pricingConfigurationRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Configuration tarifaire introuvable"
                        ));

        pricingConfigurationRepository.delete(configuration);
    }

    /**
     * Méthode qui sera utilisée par InvoiceService pour obtenir le tarif
     * applicable à une facture à une date donnée.
     */
    @Transactional(readOnly = true)
    public PricingConfiguration findApplicableConfiguration(
            InvoiceType invoiceType,
            String productName,
            SaleUnit saleUnit,
            BigDecimal packageWeightKg,
            LocalDate billingDate
    ) {
        return pricingConfigurationRepository
                .findFirstByInvoiceTypeAndProductNameAndSaleUnitAndPackageWeightKgAndActiveTrueAndDeletedFalseAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        invoiceType,
                        normalizeText(productName),
                        saleUnit,
                        normalizePackageWeight(saleUnit, packageWeightKg),
                        billingDate
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucune configuration tarifaire active ne correspond aux critères demandés"
                ));
    }

    private PricingConfiguration findActiveConfigurationById(Long id) {
        if (id == null) {
            throw badRequest("L'identifiant de la configuration est obligatoire");
        }

        return pricingConfigurationRepository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Configuration tarifaire introuvable"
                ));
    }

    private void validateBusinessRules(
            InvoiceType invoiceType,
            String productName,
            SaleUnit saleUnit,
            BigDecimal packageWeightKg,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            LocalDate effectiveFrom
    ) {
        if (invoiceType == null) {
            throw badRequest("Le type de facture est obligatoire");
        }

        if (productName == null || productName.isBlank()) {
            throw badRequest("Le nom du produit est obligatoire");
        }

        if (saleUnit == null) {
            throw badRequest("L'unité de vente est obligatoire");
        }

        if (unitPrice == null
                || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("Le prix unitaire doit être supérieur à zéro");
        }

        if (taxRate == null
                || taxRate.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("Le taux de taxe ne peut pas être négatif");
        }

        if (effectiveFrom == null) {
            throw badRequest("La date d'effet est obligatoire");
        }

        if (saleUnit == SaleUnit.SAC) {
            if (packageWeightKg == null
                    || packageWeightKg.compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest(
                        "Le poids du sac est obligatoire et doit être positif"
                );
            }
        }

        if (invoiceType == InvoiceType.MILK_PURCHASE
                && saleUnit != SaleUnit.LITRE) {
            throw badRequest(
                    "Une facture MILK_PURCHASE doit utiliser l'unité LITRE"
            );
        }
    }

    private BigDecimal normalizePackageWeight(
            SaleUnit saleUnit,
            BigDecimal packageWeightKg
    ) {
        if (saleUnit != SaleUnit.SAC) {
            return null;
        }

        return packageWeightKg.setScale(3, ROUNDING_MODE);
    }

    private BigDecimal scalePrice(BigDecimal value) {
        return value.setScale(PRICE_SCALE, ROUNDING_MODE);
    }

    private BigDecimal scaleTax(BigDecimal value) {
        return value.setScale(TAX_SCALE, ROUNDING_MODE);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private void requireManager() {
        String role = currentUserService.getCurrentRole();

        if (!"MANAGER".equals(role)
                && !"ROLE_MANAGER".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cette opération est réservée au MANAGER"
            );
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private PricingConfigurationResponse mapToResponse(
            PricingConfiguration configuration
    ) {
        return PricingConfigurationResponse.builder()
                .id(configuration.getId())
                .invoiceType(configuration.getInvoiceType())
                .productName(configuration.getProductName())
                .saleUnit(configuration.getSaleUnit())
                .packageWeightKg(configuration.getPackageWeightKg())
                .unitPrice(configuration.getUnitPrice())
                .taxRate(configuration.getTaxRate())
                .effectiveFrom(configuration.getEffectiveFrom())
                .active(configuration.isActive())
                .deleted(configuration.isDeleted())
                .deletedAt(configuration.getDeletedAt())
                .createdAt(configuration.getCreatedAt())
                .updatedAt(configuration.getUpdatedAt())
                .build();
    }
}
