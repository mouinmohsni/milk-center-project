package org.milkcenter.invoicingservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineAddedEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private Long invoiceId;
    private String invoiceNumber;
    private Long collectionId;
    private Long farmerId;
    private Integer billingMonth;
    private Integer billingYear;
    private BigDecimal quantityLiters;
    private BigDecimal lineTotalAmount;
    private LocalDateTime occurredAt;
    private String source;
}