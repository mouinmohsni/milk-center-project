package org.milkcenter.invoicingservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.milkcenter.invoicingservice.dto.request.PricingConfigurationCreateRequest;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;
import org.milkcenter.invoicingservice.model.PricingConfiguration;
import org.milkcenter.invoicingservice.repository.PricingConfigurationRepository;
import org.milkcenter.invoicingservice.security.CurrentUserService;
import org.milkcenter.invoicingservice.service.PricingConfigurationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PricingConfigurationServiceTest {

    @Mock
    private PricingConfigurationRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PricingConfigurationService service;

    @Test
    void createMilkConfigurationUsesManagerAndNormalizesPrice() {
        when(currentUserService.getCurrentRole()).thenReturn("MANAGER");
        when(repository
                .existsByInvoiceTypeAndProductNameAndSaleUnitAndPackageWeightKgAndEffectiveFromAndDeletedFalse(
                        any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(repository.save(any(PricingConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PricingConfigurationCreateRequest request =
                PricingConfigurationCreateRequest.builder()
                        .invoiceType(InvoiceType.MILK_PURCHASE)
                        .productName("  Lait cru  ")
                        .saleUnit(SaleUnit.LITRE)
                        .unitPrice(new BigDecimal("1.2"))
                        .taxRate(new BigDecimal("19"))
                        .effectiveFrom(LocalDate.of(2026, 9, 1))
                        .active(true)
                        .build();

        var response = service.create(request);

        assertEquals("Lait cru", response.getProductName());
        assertEquals(new BigDecimal("1.200"), response.getUnitPrice());
        assertEquals(new BigDecimal("19.00"), response.getTaxRate());
        verify(repository).save(any(PricingConfiguration.class));
    }

    @Test
    void createMilkConfigurationRejectsNonLitreUnit() {
        when(currentUserService.getCurrentRole()).thenReturn("MANAGER");

        PricingConfigurationCreateRequest request =
                PricingConfigurationCreateRequest.builder()
                        .invoiceType(InvoiceType.MILK_PURCHASE)
                        .productName("Lait cru")
                        .saleUnit(SaleUnit.KG)
                        .unitPrice(new BigDecimal("1.2"))
                        .taxRate(BigDecimal.ZERO)
                        .effectiveFrom(LocalDate.of(2026, 9, 1))
                        .build();

        assertThrows(ResponseStatusException.class,
                () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void findApplicableConfigurationReturnsLatestActiveConfiguration() {
        PricingConfiguration configuration = PricingConfiguration.builder()
                .id(10L)
                .invoiceType(InvoiceType.MILK_PURCHASE)
                .productName("Lait cru")
                .saleUnit(SaleUnit.LITRE)
                .unitPrice(new BigDecimal("1.300"))
                .taxRate(new BigDecimal("19.00"))
                .effectiveFrom(LocalDate.of(2026, 9, 1))
                .active(true)
                .deleted(false)
                .build();

        when(repository
                .findFirstByInvoiceTypeAndProductNameAndSaleUnitAndPackageWeightKgAndActiveTrueAndDeletedFalseAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(configuration));

        PricingConfiguration result = service.findApplicableConfiguration(
                InvoiceType.MILK_PURCHASE,
                "Lait cru",
                SaleUnit.LITRE,
                null,
                LocalDate.of(2026, 9, 1)
        );

        assertEquals(10L, result.getId());
        assertEquals(new BigDecimal("1.300"), result.getUnitPrice());
    }

    @Test
    void softDeleteDisablesConfiguration() {
        when(currentUserService.getCurrentRole()).thenReturn("MANAGER");

        PricingConfiguration configuration = PricingConfiguration.builder()
                .id(10L)
                .invoiceType(InvoiceType.MILK_PURCHASE)
                .productName("Lait cru")
                .saleUnit(SaleUnit.LITRE)
                .unitPrice(new BigDecimal("1.300"))
                .taxRate(BigDecimal.ZERO)
                .effectiveFrom(LocalDate.of(2026, 9, 1))
                .active(true)
                .deleted(false)
                .build();

        when(repository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(configuration));
        when(repository.save(any(PricingConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.softDelete(10L);

        assertEquals(false, response.isActive());
        assertEquals(true, response.isDeleted());
        verify(repository).save(configuration);
    }
}
