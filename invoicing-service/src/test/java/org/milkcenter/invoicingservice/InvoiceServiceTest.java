package org.milkcenter.invoicingservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.milkcenter.invoicingservice.service.CollectionServiceResilientClient;
import org.milkcenter.invoicingservice.dto.request.InvoiceCreateRequest;
import org.milkcenter.invoicingservice.dto.response.InvoiceResponse;
import org.milkcenter.invoicingservice.dto.response.client.MonthlyMilkTotalClientResponse;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;
import org.milkcenter.invoicingservice.model.PricingConfiguration;
import org.milkcenter.invoicingservice.repository.InvoiceRepository;
import org.milkcenter.invoicingservice.security.CurrentUserService;
import org.milkcenter.invoicingservice.service.InvoiceService;
import org.milkcenter.invoicingservice.service.PricingConfigurationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CollectionServiceResilientClient collectionServiceResilientClient;

    @Mock
    private PricingConfigurationService pricingConfigurationService;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void createMilkInvoiceUsesMonthlyCollectionTotalAndConfiguredPrice() {
        when(currentUserService.getCurrentRole()).thenReturn("MANAGER");
        when(invoiceRepository
                .existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                        20L,
                        InvoiceType.MILK_PURCHASE,
                        9,
                        2026
                ))
                .thenReturn(false);

        PricingConfiguration configuration = PricingConfiguration.builder()
                .id(7L)
                .invoiceType(InvoiceType.MILK_PURCHASE)
                .productName("Lait cru")
                .saleUnit(SaleUnit.LITRE)
                .unitPrice(new BigDecimal("1.200"))
                .taxRate(new BigDecimal("19.00"))
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .active(true)
                .deleted(false)
                .build();

        when(pricingConfigurationService.findApplicableConfiguration(
                InvoiceType.MILK_PURCHASE,
                "Lait cru",
                SaleUnit.LITRE,
                null,
                LocalDate.of(2026, 9, 1)
        )).thenReturn(configuration);

        when(collectionServiceResilientClient.getMonthlyMilkTotal(20L, 9, 2026))
                .thenReturn(new MonthlyMilkTotalClientResponse(
                        20L,
                        9,
                        2026,
                        "ACCEPTED",
                        new BigDecimal("100.000")
                ));

        when(invoiceRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceCreateRequest request = InvoiceCreateRequest.builder()
                .farmerId(20L)
                .farmerUserId(200L)
                .invoiceType(InvoiceType.MILK_PURCHASE)
                .billingMonth(9)
                .billingYear(2026)
                .build();

        InvoiceResponse response = invoiceService.createInvoice(request);

        assertEquals(1, response.getLines().size());
        assertEquals(new BigDecimal("100.000"),
                response.getLines().get(0).getQuantity());
        assertEquals(new BigDecimal("1.200"),
                response.getLines().get(0).getUnitPrice());
        assertEquals(7L,
                response.getLines().get(0).getPricingConfigurationId());
        assertEquals(new BigDecimal("142.80"),
                response.getTotalAmount());
    }
}
