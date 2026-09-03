package org.milkcenter.invoicingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.invoicingservice.dto.request.InvoiceCreateRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceStatusUpdateRequest;
import org.milkcenter.invoicingservice.dto.request.InvoiceUpdateRequest;
import org.milkcenter.invoicingservice.dto.response.InvoiceResponse;
import org.milkcenter.invoicingservice.service.InvoiceService;
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
@RequestMapping("/api/invoices" )
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /** Création d'une facture par le MANAGER. */
    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody InvoiceCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.createInvoice(request));
    }

    /** Liste globale réservée au MANAGER. */
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    /** Liste des factures du fermier connecté. */
    @GetMapping("/me")
    public ResponseEntity<List<InvoiceResponse>> getMyInvoices() {
        return ResponseEntity.ok(invoiceService.getMyInvoices());
    }

    /** Liste des factures d'un fermier, réservée au MANAGER. */
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByFarmer(
            @PathVariable Long farmerId
    ) {
        return ResponseEntity.ok(invoiceService.getInvoicesByFarmer(farmerId));
    }

    /** Consultation d'une facture précise. */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    /** Modification d'une facture encore en brouillon. */
    @PatchMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceUpdateRequest request
    ) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, request));
    }

    /** Changement contrôlé du statut d'une facture. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceResponse> updateInvoiceStatus(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                invoiceService.updateInvoiceStatus(id, request)
        );
    }

    /** Suppression d'une facture DRAFT ou CANCELLED. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable Long id
    ) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
