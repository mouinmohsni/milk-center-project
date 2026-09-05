package org.milkcenter.invoicingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingConfigurationResponse {

    private Long id;
    private InvoiceType invoiceType;
    private String productName;
    private SaleUnit saleUnit;
    private BigDecimal packageWeightKg;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private LocalDate effectiveFrom;
    private boolean active;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
