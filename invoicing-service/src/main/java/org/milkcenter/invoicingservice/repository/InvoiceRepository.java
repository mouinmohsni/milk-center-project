package org.milkcenter.invoicingservice.repository;

import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Vérifie si une facture existe déjà pour un fermier, un type et une période.
     */
    boolean existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
            Long farmerId,
            InvoiceType invoiceType,
            Integer billingMonth,
            Integer billingYear
    );

    /**
     * Recherche la facture mensuelle précise d'un fermier.
     */
    Optional<Invoice> findByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
            Long farmerId,
            InvoiceType invoiceType,
            Integer billingMonth,
            Integer billingYear
    );

    /**
     * Liste les factures d'un fermier, de la plus récente à la plus ancienne.
     */
    List<Invoice> findByFarmerIdOrderByBillingYearDescBillingMonthDesc(
            Long farmerId
    );

    /**
     * Liste les factures selon leur type.
     */
    List<Invoice> findByInvoiceTypeOrderByBillingYearDescBillingMonthDesc(
            InvoiceType invoiceType
    );
}
