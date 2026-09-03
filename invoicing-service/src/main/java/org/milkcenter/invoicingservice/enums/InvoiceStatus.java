package org.milkcenter.invoicingservice.enums;

public enum InvoiceStatus {
    /** Facture préparée mais pas encore officiellement émise. */
    DRAFT,

    /** Facture émise et en attente de paiement. */
    ISSUED,

    /** Une partie du montant a été payée. */
    PARTIALLY_PAID,

    /** Le montant total de la facture a été payé. */
    PAID,

    /** Facture annulée par un manager. */
    CANCELLED
}
