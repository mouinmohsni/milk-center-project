package org.milkcenter.invoicingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.invoicingservice.dto.request.PaymentRequest;
import org.milkcenter.invoicingservice.dto.request.PaymentStatusUpdateRequest;
import org.milkcenter.invoicingservice.dto.request.PaymentUpdateRequest;
import org.milkcenter.invoicingservice.dto.response.PaymentResponse;
import org.milkcenter.invoicingservice.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments" )
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Enregistre un paiement pour une facture. */
    @PostMapping("/invoice/{invoiceId}")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(invoiceId, request));
    }

    /** Liste les paiements d'une facture. */
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoice(
            @PathVariable Long invoiceId
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentsByInvoice(invoiceId)
        );
    }

    /** Consulte un paiement précis. */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    /** Modifie un paiement encore en attente. */
    @PatchMapping("/{id}")
    public ResponseEntity<PaymentResponse> updatePayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentUpdateRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.updatePayment(id, request)
        );
    }

    /** Confirme, refuse ou annule un paiement en attente. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody PaymentStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.updatePaymentStatus(id, request)
        );
    }

    /** Supprime un paiement qui n'est pas encore confirmé. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(
            @PathVariable Long id
    ) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
