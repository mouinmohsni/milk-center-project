package org.milkcenter.invoicingservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.milkcenter.invoicingservice.dto.request.InvoiceCreateRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceLineRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceStatusUpdateRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceUpdateRequest;
import org.milkcenter.invoicingservice.dto.response.InvoiceLineResponse;
import org.milkcenter.invoicingservice.dto.response.InvoiceResponse;
import org.milkcenter.invoicingservice.dto.response.client.MilkCollectionClientResponse;

import org.milkcenter.invoicingservice.enums.InvoiceStatus;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;
import org.milkcenter.invoicingservice.event.InvoiceNotificationEventProducer;
import org.milkcenter.invoicingservice.event.MilkCollectionStatusChangedEvent;
import org.milkcenter.invoicingservice.model.Invoice;

import org.milkcenter.invoicingservice.model.InvoiceLine;
import org.milkcenter.invoicingservice.model.PricingConfiguration;
import org.milkcenter.invoicingservice.repository.InvoiceRepository;
import org.milkcenter.invoicingservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.milkcenter.invoicingservice.repository.InvoiceLineRepository;

import java.util.Date;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private static final int MONEY_SCALE = 2;
    private static final int PRICE_SCALE = 3;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;


    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceNotificationEventProducer notificationEventProducer;







    /** Nom standard de la configuration du lait. */
    private static final String MILK_PRODUCT_NAME = "Lait cru";

    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;
    private final CollectionServiceResilientClient collectionServiceClient;
    private final PricingConfigurationService pricingConfigurationService;

    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request ) {
        requireManager();

        if (request == null) {
            throw badRequest("Les données de la facture sont obligatoires");
        }

        if (request.getInvoiceType() == null) {
            throw badRequest("Le type de facture est obligatoire");
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

    @Transactional
    public InvoiceResponse createScheduledDraftInvoice(
            Long farmerId,
            Long farmerUserId,
            Integer billingMonth,
            Integer billingYear
    ) {
        if (farmerId == null || farmerUserId == null) {
            throw badRequest("Les identifiants du farmer sont obligatoires");
        }

        validatePeriod(billingMonth, billingYear);

        boolean exists = invoiceRepository
                .existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                        farmerId,
                        InvoiceType.MILK_PURCHASE,
                        billingMonth,
                        billingYear
                );

        if (exists) {
            return invoiceRepository
                    .findByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                            farmerId,
                            InvoiceType.MILK_PURCHASE,
                            billingMonth,
                            billingYear
                    )
                    .map(this::mapToResponse)
                    .orElseThrow();
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .farmerId(farmerId)
                .farmerUserId(farmerUserId)
                .invoiceType(InvoiceType.MILK_PURCHASE)
                .status(InvoiceStatus.DRAFT)
                .billingMonth(billingMonth)
                .billingYear(billingYear)
                .taxRate(BigDecimal.ZERO.setScale(2, ROUNDING_MODE))
                .notes("Facture DRAFT créée automatiquement")
                .build();

        // La facture peut rester vide. Kafka ajoutera les lignes ACCEPTED.
        return mapToResponse(invoiceRepository.save(invoice));
    }

    /**
     * Traite une collecte ACCEPTED reçue depuis collection-service.
     * La méthode est idempotente : une collection ne peut produire
     * qu'une seule ligne dans la facture de sa période.
     */

    @Transactional
    public void processAcceptedCollection(MilkCollectionStatusChangedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("L'événement de collecte est obligatoire");
        }

        if (!"ACCEPTED".equalsIgnoreCase(event.getStatus())) {
            log.info("Événement ignoré par invoicing-service : status={}", event.getStatus());
            return;
        }

        if (event.getCollectionId() == null
                || event.getFarmerId() == null
                || event.getQuantityLiters() == null
                || event.getQuantityLiters().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "collectionId, farmerId et quantityLiters sont obligatoires et valides"
            );
        }

        LocalDate billingDate = resolveBillingDate(event);

        int month = billingDate.getMonthValue();
        int year = billingDate.getYear();

        Invoice invoice = invoiceRepository
                .findByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
                        event.getFarmerId(),
                        InvoiceType.MILK_PURCHASE,
                        month,
                        year
                )
                .orElseGet(() -> createAutomaticDraftInvoice(
                        event.getFarmerId(), month, year
                ));

        if (invoiceLineRepository.existsByInvoice_IdAndMilkCollectionId(
                invoice.getId(), event.getCollectionId())) {
            logDuplicateCollection(event, invoice);
            return;
        }

        PricingConfiguration configuration =
                pricingConfigurationService.findApplicableConfiguration(
                        InvoiceType.MILK_PURCHASE,
                        MILK_PRODUCT_NAME,
                        SaleUnit.LITRE,
                        null,
                        billingDate
                );

        InvoiceLine line = buildLineFromConfiguration(
                invoice,
                event.getCollectionId(),
                "Lait cru - collecte " + event.getCollectionId(),
                event.getQuantityLiters(),
                configuration
        );

        invoice.addLine(line);

        // Persister explicitement la ligne avant le flush de la collection Invoice.lines.
        InvoiceLine savedLine = invoiceLineRepository.saveAndFlush(line);

        recalculateTotals(invoice);
        updateInvoiceTaxRateFromLines(invoice);

        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);

        notificationEventProducer.publishInvoiceLineAdded(
                savedInvoice,
                event.getCollectionId(),
                event.getQuantityLiters(),
                savedLine.getTotalAmount()
        );
    }

    private Invoice createAutomaticDraftInvoice(Long farmerId,
                                                int billingMonth,
                                                int billingYear) {
        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .farmerId(farmerId)
                .invoiceType(InvoiceType.MILK_PURCHASE)
                .status(InvoiceStatus.DRAFT)
                .billingMonth(billingMonth)
                .billingYear(billingYear)
                .taxRate(BigDecimal.ZERO.setScale(2, ROUNDING_MODE))
                .subtotal(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE))
                .taxAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE))
                .totalAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE))
                .build();

        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);
        notificationEventProducer.publishInvoiceCreated(savedInvoice, "AUTOMATIC");

        return savedInvoice;
    }

    private LocalDate resolveBillingDate(MilkCollectionStatusChangedEvent event) {
        Date collectedAt = event.getCollectedAt();
        if (collectedAt != null) {
            return collectedAt.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }

        if (event.getValidatedAt() != null) {
            return event.getValidatedAt().toLocalDate();
        }

        return LocalDate.now();
    }

    private void logDuplicateCollection(MilkCollectionStatusChangedEvent event,
                                        Invoice invoice) {
        log.info(
                "Collection déjà facturée, événement ignoré de manière idempotente : "
                        + "collectionId={}, invoiceId={}",
                event.getCollectionId(),
                invoice.getId()
        );
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
            for (InvoiceLine existingLine : new ArrayList<>(invoice.getLines())) {
                invoice.removeLine(existingLine);
            }

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

        if (request == null || request.getStatus() == null) {
            throw badRequest("Le nouveau statut est obligatoire");
        }

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

        InvoiceStatus previousStatus = invoice.getStatus();

        invoice.setStatus(newStatus);

        Invoice invoiceUpdate = invoiceRepository.saveAndFlush(invoice);

        if (previousStatus == InvoiceStatus.DRAFT
                && newStatus == InvoiceStatus.ISSUED){

            notificationEventProducer.publishInvoiceProcessed(
                    invoiceUpdate,
                    previousStatus
            );

            log.info(
                    "Facture traitée : invoiceId={}, farmerUserId={}, totalAmount={}",
                    invoiceUpdate.getId(),
                    invoiceUpdate.getFarmerUserId(),
                    invoiceUpdate.getTotalAmount()
            );

        }

        return mapToResponse(invoiceUpdate);
    }

    @Transactional
    public InvoiceResponse cancelInvoice(Long id) {
        requireManager();

        Invoice invoice = findInvoiceById(id);

        if (invoice.getStatus() != InvoiceStatus.DRAFT
                && invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seule une facture DRAFT ou ISSUED peut être annulée"
            );
        }

        InvoiceStatus previousStatus = invoice.getStatus();

        invoice.setStatus(InvoiceStatus.CANCELLED);

        Invoice updatedInvoice = invoiceRepository.saveAndFlush(invoice);

        notificationEventProducer.publishInvoiceCancelled(
                updatedInvoice,
                previousStatus
        );

        log.info(
                "Facture annulée : invoiceId={}, farmerUserId={}, ancienStatut={}, montant={}",
                updatedInvoice.getId(),
                updatedInvoice.getFarmerUserId(),
                previousStatus,
                updatedInvoice.getTotalAmount()
        );
        return mapToResponse(updatedInvoice);
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

        List<MilkCollectionClientResponse> collections =
                collectionServiceClient.getMonthlyAcceptedCollections(
                        request.getFarmerId(),
                        request.getBillingMonth(),
                        request.getBillingYear()
                );

        if (collections == null || collections.isEmpty()) {
            throw badRequest(
                    "Aucune collecte de lait ACCEPTED n'est disponible pour cette période"
            );
        }

        Set<Long> collectionIds = new HashSet<>();

        for (MilkCollectionClientResponse collection : collections) {
            if (collection == null || collection.getId() == null) {
                throw badRequest(
                        "Une collecte reçue ne possède pas d'identifiant"
                );
            }

            if (!collectionIds.add(collection.getId())) {
                throw badRequest(
                        "La collecte #" + collection.getId()
                                + " apparaît plusieurs fois dans la réponse"
                );
            }

            if (collection.getQuantityLiters() == null
                    || collection.getQuantityLiters().compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest(
                        "La quantité d'une collecte doit être supérieure à zéro"
                );
            }

            String description = configuration.getProductName()
                    + " - collecte #"
                    + collection.getId();

            InvoiceLine line = buildLineFromConfiguration(
                    invoice,
                    collection.getId(),
                    description,
                    collection.getQuantityLiters(),
                    configuration
            );

            invoice.addLine(line);
        }
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
                            null,
                            request.getDescription(),
                            request.getQuantity(),
                            configuration
                    )

            );
        }
    }

    private InvoiceLine buildLineFromConfiguration(
            Invoice invoice,
            Long milkCollectionId,
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
                .milkCollectionId(milkCollectionId)
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




    private void validatePeriod(Integer month, Integer year) {
        if (month == null || month < 1 || month > 12) {
            throw badRequest("Le mois de facturation est invalide");
        }

        if (year == null || year < 2000 || year > 2100) {
            throw badRequest("L'année de facturation est invalide");
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
                .milkCollectionId(line.getMilkCollectionId())
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
