package org.milkcenter.invoicingservice.service;

import lombok.RequiredArgsConstructor;
import org.milkcenter.invoicingservice.dto.request.PaymentRequest;
import org.milkcenter.invoicingservice.dto.request.PaymentStatusUpdateRequest;
import org.milkcenter.invoicingservice.dto.request.PaymentUpdateRequest;
import org.milkcenter.invoicingservice.dto.response.PaymentResponse;
import org.milkcenter.invoicingservice.enums.InvoiceStatus;
import org.milkcenter.invoicingservice.enums.PaymentStatus;
import org.milkcenter.invoicingservice.model.Invoice;
import org.milkcenter.invoicingservice.model.Payment;
import org.milkcenter.invoicingservice.repository.InvoiceRepository;
import org.milkcenter.invoicingservice.repository.PaymentRepository;
import org.milkcenter.invoicingservice.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;

    /**
     * Enregistre un paiement en attente de confirmation.
     */
    @Transactional
    public PaymentResponse createPayment(Long invoiceId, PaymentRequest request ) {
        requireManager();

        Invoice invoice = findInvoiceById(invoiceId);
        validateInvoiceForPayment(invoice);

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(scale(request.getAmount()))
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .reference(request.getReference())
                .status(PaymentStatus.PENDING)
                .notes(request.getNotes())
                .build();

        return mapToResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = findPaymentById(id);
        requireReadAccess(payment.getInvoice());
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoice(Long invoiceId) {
        Invoice invoice = findInvoiceById(invoiceId);
        requireReadAccess(invoice);

        return paymentRepository.findByInvoice_IdOrderByPaymentDateDesc(invoiceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse updatePayment(Long id, PaymentUpdateRequest request) {
        requireManager();

        Payment payment = findPaymentById(id);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seul un paiement PENDING peut être modifié"
            );
        }

        if (request.getAmount() != null) {
            payment.setAmount(scale(request.getAmount()));
        }
        if (request.getPaymentDate() != null) {
            payment.setPaymentDate(request.getPaymentDate());
        }
        if (request.getPaymentMethod() != null) {
            payment.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getReference() != null) {
            payment.setReference(request.getReference());
        }
        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes());
        }

        return mapToResponse(paymentRepository.save(payment));
    }

    /**
     * Confirme ou annule un paiement, puis recalcule le statut de la facture.
     */
    @Transactional
    public PaymentResponse updatePaymentStatus(
            Long id,
            PaymentStatusUpdateRequest request
    ) {
        requireManager();

        Payment payment = findPaymentById(id);
        PaymentStatus currentStatus = payment.getStatus();
        PaymentStatus newStatus = request.getStatus();

        if (currentStatus != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seul un paiement PENDING peut changer de statut"
            );
        }

        if (newStatus == PaymentStatus.COMPLETED) {
            validatePaymentAmount(payment);
        } else if (newStatus != PaymentStatus.FAILED
                && newStatus != PaymentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Statut de paiement non autorisé depuis PENDING"
            );
        }

        payment.setStatus(newStatus);

        if (request.getReason() != null && !request.getReason().isBlank()) {
            payment.setNotes(request.getReason());
        }

        Payment savedPayment = paymentRepository.save(payment);

        if (newStatus == PaymentStatus.COMPLETED) {
            recalculateInvoiceStatus(payment.getInvoice());
        }

        return mapToResponse(savedPayment);
    }

    @Transactional
    public void deletePayment(Long id) {
        requireManager();

        Payment payment = findPaymentById(id);

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un paiement COMPLETED ne peut pas être supprimé"
            );
        }

        paymentRepository.delete(payment);
    }

    private void validateInvoiceForPayment(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.ISSUED
                && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un paiement ne peut être enregistré que pour une facture ISSUED ou PARTIALLY_PAID"
            );
        }
    }

    private void validatePaymentAmount(Payment payment) {
        Invoice invoice = payment.getInvoice();
        BigDecimal alreadyPaid = paymentRepository
                .findByInvoice_IdOrderByPaymentDateDesc(invoice.getId())
                .stream()
                .filter(existing -> existing.getStatus() == PaymentStatus.COMPLETED)
                .filter(existing -> !existing.getId().equals(payment.getId()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotal = alreadyPaid.add(payment.getAmount());

        if (newTotal.compareTo(invoice.getTotalAmount()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le total des paiements dépasse le montant de la facture"
            );
        }
    }

    private void recalculateInvoiceStatus(Invoice invoice) {
        BigDecimal paidAmount = paymentRepository
                .findByInvoice_IdOrderByPaymentDateDesc(invoice.getId())
                .stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (paidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else {
            invoice.setStatus(InvoiceStatus.ISSUED);
        }

        invoiceRepository.save(invoice);
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
                "Vous ne pouvez pas consulter ce paiement"
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

    private Invoice findInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Facture non trouvée"
                ));
    }

    private Payment findPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Paiement non trouvé"
                ));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoice().getId())
                .invoiceNumber(payment.getInvoice().getInvoiceNumber())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .reference(payment.getReference())
                .status(payment.getStatus())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
