package org.milkcenter.invoicingservice.enums;

public enum PaymentStatus {

    /** Paiement enregistré mais pas encore confirmé. */
    PENDING,

    /** Paiement confirmé. */
    COMPLETED,

    /** Paiement refusé ou échoué. */
    FAILED,

    /** Paiement annulé ou remboursé. */
    CANCELLED
}