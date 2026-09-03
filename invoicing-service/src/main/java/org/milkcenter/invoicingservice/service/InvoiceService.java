package org.milkcenter.invoicingservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.invoicingservice.dto.request.InvoiceCreateRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceLineRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceStatusUpdateRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceUpdateRequest;
import org.milkcenter.invoicingservice.dto.response.InvoiceLineResponse;
import org.milkcenter.invoicingservice.dto.response.InvoiceResponse;
import org.milkcenter.invoicingservice.enums.InvoiceStatus;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.model.Invoice;
import org.milkcenter.invoicingservice.model.InvoiceLine;
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

    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Création d'une facture par un MANAGER.
     */
    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request ) {
        requireManager();

        if (invoiceRepository.existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                request.getFarmerId(),
                request.getInvoiceType(),
                request.getBillingMonth(),
                request.getBillingYear())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une facture existe déjà pour ce fermier, ce type et cette période"
            );
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .farmerId(request.getFarmerId())
                .invoiceType(request.getInvoiceType())
                .status(InvoiceStatus.DRAFT)
                .billingMonth(request.getBillingMonth())
                .billingYear(request.getBillingYear())
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .taxRate(scale(request.getTaxRate()))
                .notes(request.getNotes())
                .build();

        List<InvoiceLineRequest> requestedLines = request.getLines() == null
                ? Collections.emptyList()
                : request.getLines();

        for (InvoiceLineRequest lineRequest : requestedLines) {
            invoice.addLine(buildLine(lineRequest, invoice.getTaxRate()));
        }

        recalculateTotals(invoice);
        return mapToResponse(invoiceRepository.save(invoice));
    }

    /**
     * Consultation d'une facture précise.
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = findInvoiceById(id);
        requireReadAccess(invoice);
        return mapToResponse(invoice);
    }

    /**
     * Liste globale réservée au MANAGER.
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        requireManager();
        return invoiceRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Liste des factures du fermier connecté.
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getMyInvoices() {
        Long farmerId = currentUserService.getCurrentUserId();

        return invoiceRepository.findByFarmerIdOrderByBillingYearDescBillingMonthDesc(farmerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Liste réservée au MANAGER pour un fermier donné.
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByFarmer(Long farmerId) {
        requireManager();

        return invoiceRepository.findByFarmerIdOrderByBillingYearDescBillingMonthDesc(farmerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Modification des informations et des lignes d'une facture encore en brouillon.
     */
    @Transactional
    public InvoiceResponse updateInvoice(Long id, InvoiceUpdateRequest request) {
        requireManager();

        Invoice invoice = findInvoiceById(id);
        ensureDraft(invoice);

        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        if (request.getLines() != null) {
            invoice.getLines().clear();

            for (InvoiceLineRequest lineRequest : request.getLines()) {
                invoice.addLine(buildLine(lineRequest, invoice.getTaxRate()));
            }

            recalculateTotals(invoice);
        }

        return mapToResponse(invoiceRepository.save(invoice));
    }

    /**
     * Modification contrôlée du statut d'une facture.
     */
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
                    "Transition de statut non autorisée : "
                            + currentStatus + " vers " + newStatus
            );
        }

        if (newStatus == InvoiceStatus.ISSUED) {
            validateBeforeIssue(invoice);
            invoice.setIssueDate(
                    invoice.getIssueDate() == null
                            ? LocalDate.now()
                            : invoice.getIssueDate()
            );
        }

        if (request.getReason() != null && !request.getReason().isBlank()) {
            invoice.setNotes(request.getReason());
        }

        invoice.setStatus(newStatus);
        return mapToResponse(invoiceRepository.save(invoice));
    }

    /**
     * Suppression réservée aux factures brouillon ou annulées.
     */
    @Transactional
    public void deleteInvoice(Long id) {
        requireManager();

        Invoice invoice = findInvoiceById(id);

        if (invoice.getStatus() != InvoiceStatus.DRAFT
                && invoice.getStatus() != InvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seules une facture DRAFT ou CANCELLED peut être supprimée"
            );
        }

        invoiceRepository.delete(invoice);
    }

    private InvoiceLine buildLine(
            InvoiceLineRequest request,
            BigDecimal invoiceTaxRate
    ) {
        BigDecimal taxRate = request.getTaxRate() == null
                ? invoiceTaxRate
                : request.getTaxRate();

        BigDecimal quantity = request.getQuantity();
        BigDecimal unitPrice = request.getUnitPrice();
        BigDecimal subtotal = quantity
                .multiply(unitPrice)
                .setScale(MONEY_SCALE, ROUNDING_MODE);
        BigDecimal taxAmount = subtotal
                .multiply(taxRate)
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, ROUNDING_MODE);
        BigDecimal totalAmount = subtotal
                .add(taxAmount)
                .setScale(MONEY_SCALE, ROUNDING_MODE);

        return InvoiceLine.builder()
                .description(request.getDescription())
                .unit(request.getUnit())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .taxRate(scale(taxRate))
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
                subtotal.add(taxAmount).setScale(MONEY_SCALE, ROUNDING_MODE)
        );
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
        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une facture doit contenir au moins une ligne avant son émission"
            );
        }

        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le montant total doit être supérieur à zéro"
            );
        }
    }

    private void requireReadAccess(Invoice invoice) {
        if (isManager()) {
            return;
        }

        if (isFarmer()
                && invoice.getFarmerId().equals(currentUserService.getCurrentUserId())) {
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

    private boolean isManager() {
        String role = currentUserService.getCurrentRole();
        return "MANAGER".equals(role) || "ROLE_MANAGER".equals(role);
    }

    private boolean isFarmer() {
        String role = currentUserService.getCurrentRole();
        return "FARMER".equals(role) || "ROLE_FARMER".equals(role);
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
                + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
        }
        return value.setScale(MONEY_SCALE, ROUNDING_MODE);
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
                .description(line.getDescription())
                .unit(line.getUnit())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .subtotal(line.getSubtotal())
                .taxRate(line.getTaxRate())
                .taxAmount(line.getTaxAmount())
                .totalAmount(line.getTotalAmount())
                .build();
    }
}
