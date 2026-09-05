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
     * Empêche deux factures du même type pour le même fermier et la même période.
     */
    boolean existsByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
            Long farmerId,
            InvoiceType invoiceType,
            Integer billingMonth,
            Integer billingYear
    );

    Optional<Invoice> findByFarmerIdAndInvoiceTypeAndBillingMonthAndBillingYear(
            Long farmerId,
            InvoiceType invoiceType,
            Integer billingMonth,
            Integer billingYear
    );

    /** Recherche par ID local du profil fermier. */
    List<Invoice> findByFarmerIdOrderByBillingYearDescBillingMonthDesc(
            Long farmerId
    );

    /**
     * Recherche par userId Identity, utilisé pour /api/invoices/me.
     */
    List<Invoice> findByFarmerUserIdOrderByBillingYearDescBillingMonthDesc(
            Long farmerUserId
    );

    List<Invoice> findByInvoiceTypeOrderByBillingYearDescBillingMonthDesc(
            InvoiceType invoiceType
    );
}
