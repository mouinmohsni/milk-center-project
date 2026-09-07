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
     *
     * Les paiements PENDING et COMPLETED réservent une partie du montant
     * de la facture. Les paiements FAILED et CANCELLED ne sont pas comptés.
     */
    @Transactional
    public PaymentResponse createPayment(
            Long invoiceId,
            PaymentRequest request
    ) {
        requireManager();

        Invoice invoice = findInvoiceById(invoiceId);
        validateInvoiceForPayment(invoice);

        BigDecimal amount = scale(request.getAmount());

        validateReferenceForCreate(request.getReference());
        validatePaymentAmountForInvoice(invoice, amount, null);

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(amount)
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .reference(request.getReference())
                .status(PaymentStatus.PENDING)
                .notes(request.getNotes())
                .build();

        return mapToResponse(paymentRepository.save(payment));
    }

    /**
     * Consulte un paiement précis.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = findPaymentById(id);
        requireReadAccess(payment.getInvoice());
        return mapToResponse(payment);
    }

    /**
     * Liste les paiements d'une facture.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoice(Long invoiceId) {
        Invoice invoice = findInvoiceById(invoiceId);
        requireReadAccess(invoice);

        return paymentRepository
                .findByInvoice_IdOrderByPaymentDateDesc(invoiceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Modifie uniquement un paiement encore en attente.
     */
    @Transactional
    public PaymentResponse updatePayment(
            Long id,
            PaymentUpdateRequest request
    ) {
        requireManager();

        Payment payment = findPaymentById(id);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seul un paiement PENDING peut être modifié"
            );
        }

        BigDecimal newAmount = payment.getAmount();

        if (request.getAmount() != null) {
            newAmount = scale(request.getAmount());
        }

        // Le paiement en cours est exclu du cumul afin de ne pas le compter deux fois.
        validatePaymentAmountForInvoice(
                payment.getInvoice(),
                newAmount,
                payment.getId()
        );

        if (request.getReference() != null) {
            validateReferenceForUpdate(
                    request.getReference(),
                    payment.getId()
            );
        }

        payment.setAmount(newAmount);

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
     * Confirme, refuse ou annule un paiement en attente.
     */
    @Transactional
    public PaymentResponse updatePaymentStatus(
            Long id,
            PaymentStatusUpdateRequest request
    ) {
        requireManager();

        Payment payment = findPaymentById(id);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seul un paiement PENDING peut changer de statut"
            );
        }

        PaymentStatus newStatus = request.getStatus();

        if (newStatus == PaymentStatus.COMPLETED) {
            validatePaymentAmount(payment);
        } else if (newStatus != PaymentStatus.FAILED
                && newStatus != PaymentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Statut non autorisé depuis PENDING"
            );
        }

        payment.setStatus(newStatus);

        if (request.getReason() != null
                && !request.getReason().isBlank()) {
            payment.setNotes(request.getReason());
        }

        Payment savedPayment = paymentRepository.save(payment);

        if (newStatus == PaymentStatus.COMPLETED) {
            recalculateInvoiceStatus(payment.getInvoice());
        }

        return mapToResponse(savedPayment);
    }

    /**
     * Supprime uniquement un paiement qui n'est pas confirmé.
     */
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

    /**
     * Vérifie qu'une facture peut recevoir un paiement.
     */
    private void validateInvoiceForPayment(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.ISSUED
                && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un paiement nécessite une facture ISSUED ou PARTIALLY_PAID"
            );
        }
    }

    /**
     * Vérifie le montant au moment de la confirmation d'un paiement.
     */
    private void validatePaymentAmount(Payment payment) {
        validatePaymentAmountForInvoice(
                payment.getInvoice(),
                payment.getAmount(),
                payment.getId()
        );
    }

    /**
     * Vérifie que le cumul des paiements réservés ne dépasse pas la facture.
     *
     * Les paiements PENDING et COMPLETED sont comptés.
     * Les paiements FAILED et CANCELLED sont ignorés.
     *
     * @param invoice facture concernée
     * @param newAmount nouveau montant à réserver
     * @param excludedPaymentId paiement à exclure lors d'une mise à jour
     */
    private void validatePaymentAmountForInvoice(
            Invoice invoice,
            BigDecimal newAmount,
            Long excludedPaymentId
    ) {
        if (newAmount == null
                || newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le montant du paiement doit être supérieur à zéro"
            );
        }

        BigDecimal alreadyReserved = paymentRepository
                .findByInvoice_IdOrderByPaymentDateDesc(invoice.getId())
                .stream()
                .filter(payment ->
                        payment.getStatus() == PaymentStatus.PENDING
                                || payment.getStatus()
                                == PaymentStatus.COMPLETED
                )
                .filter(payment -> excludedPaymentId == null
                        || !payment.getId().equals(excludedPaymentId))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotal = alreadyReserved.add(newAmount);

        if (newTotal.compareTo(invoice.getTotalAmount()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le total des paiements en attente et confirmés "
                            + "dépasse le montant de la facture"
            );
        }
    }

    /**
     * Vérifie l'unicité de la référence lors d'une création.
     */
    private void validateReferenceForCreate(String reference) {
        if (reference == null || reference.isBlank()) {
            return;
        }

        if (paymentRepository.existsByReference(reference)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette référence de paiement existe déjà"
            );
        }
    }

    /**
     * Vérifie l'unicité de la référence lors d'une modification.
     */
    private void validateReferenceForUpdate(
            String reference,
            Long paymentId
    ) {
        if (reference == null || reference.isBlank()) {
            return;
        }

        if (paymentRepository.existsByReferenceAndIdNot(
                reference,
                paymentId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette référence de paiement existe déjà"
            );
        }
    }

    /**
     * Recalcule le statut de la facture après la confirmation d'un paiement.
     */
    private void recalculateInvoiceStatus(Invoice invoice) {
        BigDecimal paidAmount = paymentRepository
                .findByInvoice_IdOrderByPaymentDateDesc(invoice.getId())
                .stream()
                .filter(payment -> payment.getStatus()
                        == PaymentStatus.COMPLETED)
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

    /**
     * MANAGER : accès global.
     * FARMER : accès uniquement aux paiements de ses propres factures.
     */
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
                "Vous ne pouvez pas consulter ce paiement"
        );
    }

    /**
     * Vérifie que l'utilisateur connecté est manager.
     */
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
        return "MANAGER".equals(role)
                || "ROLE_MANAGER".equals(role);
    }

    private boolean isFarmer() {
        String role = currentUserService.getCurrentRole();
        return "FARMER".equals(role)
                || "ROLE_FARMER".equals(role);
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
        if (value == null) {
            return BigDecimal.ZERO.setScale(
                    MONEY_SCALE,
                    ROUNDING_MODE
            );
        }

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