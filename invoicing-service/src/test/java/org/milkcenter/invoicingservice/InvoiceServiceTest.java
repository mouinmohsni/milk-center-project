package org.milkcenter.invoicingservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.milkcenter.invoicingservice.dto.request.InvoiceCreateRequest;
import org.milkcenter.invoicingservice.dto.response.InvoiceResponse;
import org.milkcenter.invoicingservice.dto.response.client.MilkCollectionClientResponse;
import org.milkcenter.invoicingservice.enums.InvoiceStatus;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;
import org.milkcenter.invoicingservice.model.Invoice;
import org.milkcenter.invoicingservice.model.PricingConfiguration;
import org.milkcenter.invoicingservice.repository.InvoiceRepository;
import org.milkcenter.invoicingservice.security.CurrentUserService;
import org.milkcenter.invoicingservice.service.CollectionServiceResilientClient;
import org.milkcenter.invoicingservice.service.InvoiceService;
import org.milkcenter.invoicingservice.service.PricingConfigurationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void createMilkInvoiceCreatesOneLinePerAcceptedCollection() {
        when(currentUserService.getCurrentRole()).thenReturn("MANAGER");
        when(invoiceRepository
                .existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                        20L,
                        InvoiceType.MILK_PURCHASE,
                        9,
                        2026
                ))
                .thenReturn(false);

        when(pricingConfigurationService.findApplicableConfiguration(
                InvoiceType.MILK_PURCHASE,
                "Lait cru",
                SaleUnit.LITRE,
                null,
                LocalDate.of(2026, 9, 1)
        )).thenReturn(buildMilkConfiguration());

        MilkCollectionClientResponse firstCollection =
                buildCollection(101L, "100.000");
        MilkCollectionClientResponse secondCollection =
                buildCollection(102L, "50.000");

        when(collectionServiceResilientClient.getMonthlyAcceptedCollections(
                20L,
                9,
                2026
        )).thenReturn(List.of(firstCollection, secondCollection));

        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = invoiceService.createInvoice(
                buildMilkInvoiceRequest()
        );

        assertEquals(2, response.getLines().size());
        assertEquals(101L, response.getLines().get(0).getMilkCollectionId());
        assertEquals(102L, response.getLines().get(1).getMilkCollectionId());
        assertEquals(
                new BigDecimal("100.000"),
                response.getLines().get(0).getQuantity()
        );
        assertEquals(
                new BigDecimal("50.000"),
                response.getLines().get(1).getQuantity()
        );
        assertEquals(
                new BigDecimal("1.200"),
                response.getLines().get(0).getUnitPrice()
        );
        assertEquals(
                7L,
                response.getLines().get(0).getPricingConfigurationId()
        );
        assertEquals(new BigDecimal("214.20"), response.getTotalAmount());
    }

    @Test
    void createMilkInvoiceRejectsDuplicateCollectionId() {
        when(currentUserService.getCurrentRole()).thenReturn("MANAGER");
        when(invoiceRepository
                .existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                        20L,
                        InvoiceType.MILK_PURCHASE,
                        9,
                        2026
                ))
                .thenReturn(false);

        when(pricingConfigurationService.findApplicableConfiguration(
                InvoiceType.MILK_PURCHASE,
                "Lait cru",
                SaleUnit.LITRE,
                null,
                LocalDate.of(2026, 9, 1)
        )).thenReturn(buildMilkConfiguration());

        MilkCollectionClientResponse duplicateFirst =
                buildCollection(101L, "100.000");
        MilkCollectionClientResponse duplicateSecond =
                buildCollection(101L, "50.000");

        when(collectionServiceResilientClient.getMonthlyAcceptedCollections(
                20L,
                9,
                2026
        )).thenReturn(List.of(duplicateFirst, duplicateSecond));

        assertThrows(
                ResponseStatusException.class,
                () -> invoiceService.createInvoice(buildMilkInvoiceRequest())
        );

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void createScheduledDraftInvoiceReturnsExistingInvoiceWithoutDuplicate() {
        Invoice existingInvoice = Invoice.builder()
                .id(10L)
                .invoiceNumber("FAC-EXISTANTE")
                .farmerId(20L)
                .farmerUserId(200L)
                .invoiceType(InvoiceType.MILK_PURCHASE)
                .status(InvoiceStatus.DRAFT)
                .billingMonth(9)
                .billingYear(2026)
                .build();

        when(invoiceRepository
                .existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                        20L,
                        InvoiceType.MILK_PURCHASE,
                        9,
                        2026
                ))
                .thenReturn(true);

        when(invoiceRepository
                .findByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                        20L,
                        InvoiceType.MILK_PURCHASE,
                        9,
                        2026
                ))
                .thenReturn(Optional.of(existingInvoice));

        InvoiceResponse response = invoiceService.createScheduledDraftInvoice(
                20L,
                200L,
                9,
                2026
        );

        assertEquals(10L, response.getId());
        assertEquals("FAC-EXISTANTE", response.getInvoiceNumber());
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void createScheduledDraftInvoiceCreatesEmptyDraftWhenAbsent() {
        when(invoiceRepository
                .existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                        20L,
                        InvoiceType.MILK_PURCHASE,
                        9,
                        2026
                ))
                .thenReturn(false);

        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> {
                    Invoice invoice = invocation.getArgument(0);
                    invoice.setId(11L);
                    return invoice;
                });

        InvoiceResponse response = invoiceService.createScheduledDraftInvoice(
                20L,
                200L,
                9,
                2026
        );

        assertEquals(11L, response.getId());
        assertEquals(20L, response.getFarmerId());
        assertEquals(200L, response.getFarmerUserId());
        assertEquals(InvoiceType.MILK_PURCHASE, response.getInvoiceType());
        assertEquals(InvoiceStatus.DRAFT, response.getStatus());
        assertEquals(9, response.getBillingMonth());
        assertEquals(2026, response.getBillingYear());
        assertEquals(0, response.getLines().size());

        verify(invoiceRepository).save(any(Invoice.class));
    }

    private InvoiceCreateRequest buildMilkInvoiceRequest() {
        return InvoiceCreateRequest.builder()
                .farmerId(20L)
                .farmerUserId(200L)
                .invoiceType(InvoiceType.MILK_PURCHASE)
                .billingMonth(9)
                .billingYear(2026)
                .build();
    }

    private PricingConfiguration buildMilkConfiguration() {
        return PricingConfiguration.builder()
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
    }

    private MilkCollectionClientResponse buildCollection(
            Long id,
            String quantity
    ) {
        return new MilkCollectionClientResponse(
                id,
                20L,
                30L,
                40L,
                new Date(),
                new BigDecimal(quantity),
                new BigDecimal("4.200"),
                "Qualité normale",
                "ACCEPTED",
                "Test"
        );
    }
}

// Fin du fichier
