package org.milkcenter.invoicingservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreatedEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private Long invoiceId;
    private String invoiceNumber;
    private Long farmerId;
    private Integer billingMonth;
    private Integer billingYear;
    private String invoiceType;
    private String creationMode;
    private LocalDateTime occurredAt;
}