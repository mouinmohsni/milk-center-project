package org.milkcenter.invoicingservice.repository;

import org.milkcenter.invoicingservice.model.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    /**
     * Retourne toutes les lignes appartenant à une facture.
     */
    List<InvoiceLine> findByInvoice_Id(Long invoiceId);
}
