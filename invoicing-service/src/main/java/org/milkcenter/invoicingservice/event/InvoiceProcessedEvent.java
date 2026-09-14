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
public class InvoiceProcessedEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private Long invoiceId;
    private String invoiceNumber;
    private Long farmerId;
    private Long farmerUserId;
    private Integer billingMonth;
    private Integer billingYear;
    private String previousStatus;
    private String newStatus;
    private BigDecimal totalAmount;
    private LocalDateTime processedAt;
    private String source;
}
