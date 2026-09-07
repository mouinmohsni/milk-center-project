package org.milkcenter.invoicingservice.repository;

import org.milkcenter.invoicingservice.enums.PaymentStatus;
import org.milkcenter.invoicingservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoice_IdOrderByPaymentDateDesc(Long invoiceId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByReference(String reference);

    boolean existsByReferenceAndIdNot(String reference, Long id);
}
