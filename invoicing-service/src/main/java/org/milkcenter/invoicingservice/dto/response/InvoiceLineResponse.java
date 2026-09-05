package org.milkcenter.invoicingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineResponse {

    private Long id;

    /** Référence logique de la configuration utilisée. */
    private Long pricingConfigurationId;

    private String description;
    private String unit;
    private BigDecimal packageWeightKg;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
}
