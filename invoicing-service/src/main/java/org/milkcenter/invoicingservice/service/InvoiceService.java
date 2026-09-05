package org.milkcenter.invoicingservice.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.milkcenter.invoicingservice.client.CollectionServiceClient;
import org.milkcenter.invoicingservice.dto.request.InvoiceCreateRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceLineRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceStatusUpdateRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceUpdateRequest;
import org.milkcenter.invoicingservice.dto.response.InvoiceLineResponse;
import org.milkcenter.invoicingservice.dto.response.InvoiceResponse;
import org.milkcenter.invoicingservice.dto.response.client.MonthlyMilkTotalClientResponse;
import org.milkcenter.invoicingservice.enums.InvoiceStatus;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;
import org.milkcenter.invoicingservice.model.Invoice;
import org.milkcenter.invoicingservice.model.InvoiceLine;
import org.milkcenter.invoicingservice.model.PricingConfiguration;
import org.milkcenter.invoicingservice.repository.InvoiceRepository;
import org.milkcenter.invoicingservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final int MONEY_SCALE = 2;
    private static final int PRICE_SCALE = 3;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /** Nom standard de la configuration du lait. */
    private static final String MILK_PRODUCT_NAME = "Lait cru";

    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;
    private final CollectionServiceClient collectionServiceClient;
    private final PricingConfigurationService pricingConfigurationService;

    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request ) {
        requireManager();

        if (request == null) {
            throw badRequest("Les données de la facture sont obligatoires");
        }

        boolean exists = invoiceRepository
                .existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                        request.getFarmerId(),
                        request.getInvoiceType(),
                        request.getBillingMonth(),
                        request.getBillingYear()
                );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une facture existe déjà pour ce fermier, ce type et cette période"
            );
        }

        validateInvoicePeriod(request);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .farmerId(request.getFarmerId())
                .farmerUserId(request.getFarmerUserId())
                .invoiceType(request.getInvoiceType())
                .status(InvoiceStatus.DRAFT)
                .billingMonth(request.getBillingMonth())
                .billingYear(request.getBillingYear())
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .taxRate(scaleTax(request.getTaxRate()))
                .notes(request.getNotes())
                .build();

        if (request.getInvoiceType() == InvoiceType.MILK_PURCHASE) {
            addMilkLine(invoice, request);
        } else if (request.getInvoiceType() == InvoiceType.FEED_SALE) {
            addFeedLines(invoice, request.getLines());
        } else {
            throw badRequest("Type de facture non pris en charge");
        }

        updateInvoiceTaxRateFromLines(invoice);
        recalculateTotals(invoice);

        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = findInvoiceById(id);
        requireReadAccess(invoice);
        return mapToResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        requireManager();

        return invoiceRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getMyInvoices() {
        requireFarmer();

        Long farmerUserId = currentUserService.getCurrentUserId();

        return invoiceRepository
                .findByFarmerUserIdOrderByBillingYearDescBillingMonthDesc(
                        farmerUserId
                )
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByFarmer(Long farmerId) {
        requireManager();

        return invoiceRepository
                .findByFarmerIdOrderByBillingYearDescBillingMonthDesc(farmerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse updateInvoice(
            Long id,
            InvoiceUpdateRequest request
    ) {
        requireManager();

        Invoice invoice = findInvoiceById(id);
        ensureDraft(invoice);

        if (request == null) {
            throw badRequest("Les données de modification sont obligatoires");
        }

        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        if (request.getLines() != null) {
            invoice.getLines().clear();

            if (invoice.getInvoiceType() == InvoiceType.MILK_PURCHASE) {
                addMilkLine(invoice, createInternalRequestFromInvoice(invoice));
            } else if (invoice.getInvoiceType() == InvoiceType.FEED_SALE) {
                addFeedLines(invoice, request.getLines());
            }

            updateInvoiceTaxRateFromLines(invoice);
            recalculateTotals(invoice);
        }

        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse updateInvoiceStatus(
            Long id,
            InvoiceStatusUpdateRequest request
    ) {
        requireManager();

        Invoice invoice = findInvoiceById(id);
        InvoiceStatus currentStatus = invoice.getStatus();
        InvoiceStatus newStatus = request.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transition non autorisée : "
                            + currentStatus + " vers " + newStatus
            );
        }

        if (newStatus == InvoiceStatus.ISSUED) {
            validateBeforeIssue(invoice);

            if (invoice.getIssueDate() == null) {
                invoice.setIssueDate(LocalDate.now());
            }
        }

        if (request.getReason() != null
                && !request.getReason().isBlank()) {
            invoice.setNotes(request.getReason());
        }

        invoice.setStatus(newStatus);
        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public void deleteInvoice(Long id) {
        requireManager();

        Invoice invoice = findInvoiceById(id);

        if (invoice.getStatus() != InvoiceStatus.DRAFT
                && invoice.getStatus() != InvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seule une facture DRAFT ou CANCELLED peut être supprimée"
            );
        }

        invoiceRepository.delete(invoice);
    }

    private void addMilkLine(
            Invoice invoice,
            InvoiceCreateRequest request
    ) {
        LocalDate billingDate = LocalDate.of(
                request.getBillingYear(),
                request.getBillingMonth(),
                1
        );

        PricingConfiguration configuration =
                pricingConfigurationService.findApplicableConfiguration(
                        InvoiceType.MILK_PURCHASE,
                        MILK_PRODUCT_NAME,
                        SaleUnit.LITRE,
                        null,
                        billingDate
                );

        MonthlyMilkTotalClientResponse milkTotal;

        try {
            milkTotal = collectionServiceClient.getMonthlyMilkTotal(
                    request.getFarmerId(),
                    request.getBillingMonth(),
                    request.getBillingYear()
            );
        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Impossible de récupérer le total mensuel de lait"
            );
        }

        if (milkTotal == null
                || milkTotal.getTotalQuantityLiters() == null
                || milkTotal.getTotalQuantityLiters()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest(
                    "Aucune quantité de lait ACCEPTED n'est disponible pour cette période"
            );
        }

        InvoiceLine line = buildLineFromConfiguration(
                invoice,
                configuration.getProductName()
                        + " - "
                        + request.getBillingMonth()
                        + "/"
                        + request.getBillingYear(),
                milkTotal.getTotalQuantityLiters(),
                configuration
        );

        invoice.addLine(line);
    }

    private void addFeedLines(
            Invoice invoice,
            List<InvoiceLineRequest> lineRequests
    ) {
        List<InvoiceLineRequest> lines = lineRequests == null
                ? Collections.emptyList()
                : lineRequests;

        if (lines.isEmpty()) {
            throw badRequest(
                    "Une facture FEED_SALE doit contenir au moins une ligne"
            );
        }

        for (InvoiceLineRequest request : lines) {
            if (request.getUnit() == null
                    || request.getUnit().isBlank()) {
                throw badRequest(
                        "L'unité est obligatoire pour une ligne d'aliment"
                );
            }

            SaleUnit saleUnit = parseSaleUnit(request.getUnit());

            PricingConfiguration configuration =
                    pricingConfigurationService.findApplicableConfiguration(
                            InvoiceType.FEED_SALE,
                            request.getDescription(),
                            saleUnit,
                            request.getPackageWeightKg(),
                            LocalDate.of(
                                    invoice.getBillingYear(),
                                    invoice.getBillingMonth(),
                                    1
                            )
                    );

            invoice.addLine(
                    buildLineFromConfiguration(
                            invoice,
                            request.getDescription(),
                            request.getQuantity(),
                            configuration
                    )
            );
        }
    }

    private InvoiceLine buildLineFromConfiguration(
            Invoice invoice,
            String description,
            BigDecimal quantity,
            PricingConfiguration configuration
    ) {
        if (quantity == null
                || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("La quantité doit être supérieure à zéro");
        }

        BigDecimal unitPrice = configuration.getUnitPrice()
                .setScale(PRICE_SCALE, ROUNDING_MODE);
        BigDecimal taxRate = configuration.getTaxRate()
                .setScale(2, ROUNDING_MODE);

        BigDecimal subtotal = quantity
                .multiply(unitPrice)
                .setScale(MONEY_SCALE, ROUNDING_MODE);

        BigDecimal taxAmount = subtotal
                .multiply(taxRate)
                .divide(
                        BigDecimal.valueOf(100),
                        MONEY_SCALE,
                        ROUNDING_MODE
                );

        BigDecimal totalAmount = subtotal
                .add(taxAmount)
                .setScale(MONEY_SCALE, ROUNDING_MODE);

        return InvoiceLine.builder()
                .invoice(invoice)
                .pricingConfigurationId(configuration.getId())
                .description(description)
                .unit(configuration.getSaleUnit().name())
                .packageWeightKg(configuration.getPackageWeightKg())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .build();
    }

    private void recalculateTotals(Invoice invoice) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (InvoiceLine line : invoice.getLines()) {
            subtotal = subtotal.add(line.getSubtotal());
            taxAmount = taxAmount.add(line.getTaxAmount());
        }

        invoice.setSubtotal(subtotal.setScale(MONEY_SCALE, ROUNDING_MODE));
        invoice.setTaxAmount(taxAmount.setScale(MONEY_SCALE, ROUNDING_MODE));
        invoice.setTotalAmount(
                subtotal.add(taxAmount)
                        .setScale(MONEY_SCALE, ROUNDING_MODE)
        );
    }

    private void updateInvoiceTaxRateFromLines(Invoice invoice) {
        if (invoice.getLines() != null
                && !invoice.getLines().isEmpty()) {
            invoice.setTaxRate(
                    invoice.getLines()
                            .get(0)
                            .getTaxRate()
            );
        }
    }

    private InvoiceCreateRequest createInternalRequestFromInvoice(
            Invoice invoice
    ) {
        return InvoiceCreateRequest.builder()
                .farmerId(invoice.getFarmerId())
                .farmerUserId(invoice.getFarmerUserId())
                .invoiceType(invoice.getInvoiceType())
                .billingMonth(invoice.getBillingMonth())
                .billingYear(invoice.getBillingYear())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .taxRate(invoice.getTaxRate())
                .notes(invoice.getNotes())
                .lines(new ArrayList<>())
                .build();
    }

    private SaleUnit parseSaleUnit(String value) {
        try {
            return SaleUnit.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw badRequest(
                    "Unité de vente invalide : " + value
            );
        }
    }

    private boolean isValidTransition(
            InvoiceStatus currentStatus,
            InvoiceStatus newStatus
    ) {
        if (currentStatus == InvoiceStatus.DRAFT) {
            return newStatus == InvoiceStatus.ISSUED
                    || newStatus == InvoiceStatus.CANCELLED;
        }

        if (currentStatus == InvoiceStatus.ISSUED) {
            return newStatus == InvoiceStatus.CANCELLED;
        }

        return false;
    }

    private void validateBeforeIssue(Invoice invoice) {
        if (invoice.getLines() == null
                || invoice.getLines().isEmpty()) {
            throw badRequest(
                    "Une facture doit contenir au moins une ligne"
            );
        }

        recalculateTotals(invoice);

        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest(
                    "Le montant total doit être supérieur à zéro"
            );
        }
    }

    private void validateInvoicePeriod(InvoiceCreateRequest request) {
        if (request.getBillingMonth() == null
                || request.getBillingMonth() < 1
                || request.getBillingMonth() > 12) {
            throw badRequest("Le mois de facturation est invalide");
        }

        if (request.getBillingYear() == null
                || request.getBillingYear() < 2000
                || request.getBillingYear() > 2100) {
            throw badRequest("L'année de facturation est invalide");
        }
    }

    private void requireReadAccess(Invoice invoice) {
        if (isManager()) {
            return;
        }

        Long currentUserId = currentUserService.getCurrentUserId();

        if (isFarmer()
                && invoice.getFarmerUserId() != null
                && invoice.getFarmerUserId().equals(currentUserId)) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Vous ne pouvez pas consulter cette facture"
        );
    }

    private void requireManager() {
        if (!isManager()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cette opération est réservée au MANAGER"
            );
        }
    }

    private void requireFarmer() {
        if (!isFarmer()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cette opération est réservée au FARMER"
            );
        }
    }

    private boolean isManager() {
        String role = currentUserService.getCurrentRole();
        return "MANAGER".equals(role)
                || "ROLE_MANAGER".equals(role);
    }

    private boolean isFarmer() {
        String role = currentUserService.getCurrentRole();
        return "FARMER".equals(role)
                || "ROLE_FARMER".equals(role);
    }

    private void ensureDraft(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seule une facture DRAFT peut être modifiée"
            );
        }
    }

    private Invoice findInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Facture non trouvée"
                ));
    }

    private String generateInvoiceNumber() {
        return "FAC-"
                + LocalDate.now().getYear()
                + "-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private BigDecimal scaleTax(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, ROUNDING_MODE);
        }

        return value.setScale(2, ROUNDING_MODE);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        List<InvoiceLineResponse> lines = invoice.getLines() == null
                ? new ArrayList<>()
                : invoice.getLines()
                .stream()
                .map(this::mapLineToResponse)
                .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .farmerId(invoice.getFarmerId())
                .farmerUserId(invoice.getFarmerUserId())
                .invoiceType(invoice.getInvoiceType())
                .status(invoice.getStatus())
                .billingMonth(invoice.getBillingMonth())
                .billingYear(invoice.getBillingYear())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal())
                .taxRate(invoice.getTaxRate())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .notes(invoice.getNotes())
                .lines(lines)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    private InvoiceLineResponse mapLineToResponse(InvoiceLine line) {
        return InvoiceLineResponse.builder()
                .id(line.getId())
                .pricingConfigurationId(line.getPricingConfigurationId())
                .description(line.getDescription())
                .unit(line.getUnit())
                .packageWeightKg(line.getPackageWeightKg())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .subtotal(line.getSubtotal())
                .taxRate(line.getTaxRate())
                .taxAmount(line.getTaxAmount())
                .totalAmount(line.getTotalAmount())
                .build();
    }
}
