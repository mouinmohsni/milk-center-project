package org.milkcenter.invoicingservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.milkcenter.invoicingservice.enums.InvoiceStatus;
import org.milkcenter.invoicingservice.model.Invoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceNotificationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.invoice-notifications:invoice-notification-events}")
    private String invoiceNotificationsTopic;

    @Value("${app.kafka.topics.invoice-line-notifications:invoice-line-notification-events}")
    private String invoiceLineNotificationsTopic;

    @Value("${app.kafka.topics.invoice-processing-reminders:invoice-processing-reminder-events}")
    private String invoiceProcessingReminderTopic;

    @Value("${app.kafka.topics.invoice-processed:invoice-processed-events}")
    private String invoiceProcessedTopic;

    @Value("${app.kafka.topics.invoice-cancelled:invoice-cancelled-events}")
    private String invoiceCancelledTopic;




    public void publishInvoiceCreated(Invoice invoice, String creationMode) {
        InvoiceCreatedEvent event = InvoiceCreatedEvent.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .farmerId(invoice.getFarmerId())
                .billingMonth(invoice.getBillingMonth())
                .billingYear(invoice.getBillingYear())
                .invoiceType(invoice.getInvoiceType().name())
                .creationMode(creationMode)
                .occurredAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(
                invoiceNotificationsTopic,
                String.valueOf(invoice.getId()),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Échec de publication de InvoiceCreatedEvent pour invoiceId={}",
                        invoice.getId(), exception);
            } else {
                log.info("InvoiceCreatedEvent publié : invoiceId={}", invoice.getId());
            }
        });
    }

    public void publishInvoiceLineAdded(Invoice invoice,
                                        Long collectionId,
                                        java.math.BigDecimal quantityLiters,
                                        java.math.BigDecimal lineTotalAmount) {
        InvoiceLineAddedEvent event = InvoiceLineAddedEvent.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .collectionId(collectionId)
                .farmerId(invoice.getFarmerId())
                .billingMonth(invoice.getBillingMonth())
                .billingYear(invoice.getBillingYear())
                .quantityLiters(quantityLiters)
                .lineTotalAmount(lineTotalAmount)
                .occurredAt(LocalDateTime.now())
                .source("invoicing-service")
                .build();

        kafkaTemplate.send(
                invoiceLineNotificationsTopic,
                String.valueOf(collectionId),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Échec de publication de InvoiceLineAddedEvent pour collectionId={}",
                        collectionId, exception);
            } else {
                log.info("InvoiceLineAddedEvent publié : collectionId={}", collectionId);
            }
        });
    }


    public void publishInvoiceProcessingReminder(Invoice invoice) {
        InvoiceProcessingReminderEvent event =
                InvoiceProcessingReminderEvent.builder()
                        .invoiceId(invoice.getId())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .farmerId(invoice.getFarmerId())
                        .billingMonth(invoice.getBillingMonth())
                        .billingYear(invoice.getBillingYear())
                        .status(invoice.getStatus().name())
                        .occurredAt(LocalDateTime.now())
                        .source("invoicing-service")
                        .build();

        kafkaTemplate.send(
                invoiceProcessingReminderTopic,
                String.valueOf(invoice.getId()),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Échec publication InvoiceProcessingReminderEvent invoiceId={}",
                        invoice.getId(),
                        exception
                );
            } else {
                log.info(
                        "InvoiceProcessingReminderEvent publié : invoiceId={}",
                        invoice.getId()
                );
            }
        });
    }

    public void publishInvoiceProcessed(Invoice invoice,
                                        InvoiceStatus previousStatus) {

        InvoiceProcessedEvent event = InvoiceProcessedEvent.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .farmerId(invoice.getFarmerId())
                .farmerUserId(invoice.getFarmerUserId())
                .billingMonth(invoice.getBillingMonth())
                .billingYear(invoice.getBillingYear())
                .previousStatus(previousStatus.name())
                .newStatus(invoice.getStatus().name())
                .totalAmount(invoice.getTotalAmount())
                .processedAt(LocalDateTime.now())
                .source("invoicing-service")
                .build();

        kafkaTemplate.send(
                invoiceProcessedTopic,
                String.valueOf(invoice.getId()),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Échec publication InvoiceProcessedEvent invoiceId={}",
                        invoice.getId(),
                        exception
                );
            } else {
                log.info(
                        "InvoiceProcessedEvent publié : invoiceId={}, farmerUserId={}",
                        invoice.getId(),
                        invoice.getFarmerUserId()
                );
            }
        });
    }

    public void publishInvoiceCancelled(Invoice invoice,
                                        InvoiceStatus previousStatus) {

        InvoiceCancelledEvent event = InvoiceCancelledEvent.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .farmerId(invoice.getFarmerId())
                .farmerUserId(invoice.getFarmerUserId())
                .billingMonth(invoice.getBillingMonth())
                .billingYear(invoice.getBillingYear())
                .previousStatus(previousStatus.name())
                .newStatus(invoice.getStatus().name())
                .totalAmount(invoice.getTotalAmount())
                .cancelledAt(LocalDateTime.now())
                .source("invoicing-service")
                .build();

        kafkaTemplate.send(
                invoiceCancelledTopic,
                String.valueOf(invoice.getId()),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Échec publication InvoiceCancelledEvent invoiceId={}",
                        invoice.getId(),
                        exception
                );
            } else {
                log.info(
                        "InvoiceCancelledEvent publié : invoiceId={}",
                        invoice.getId()
                );
            }
        });
    }



}