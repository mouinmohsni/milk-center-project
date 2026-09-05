package org.milkcenter.invoicingservice.dto.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.milkcenter.invoicingservice.enums.InvoiceType;
import org.milkcenter.invoicingservice.enums.SaleUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingConfigurationPatchRequest {

    private InvoiceType invoiceType;

    @Size(max = 150, message = "Le nom du produit ne doit pas dépasser 150 caractères")
    private String productName;

    private SaleUnit saleUnit;

    /** Obligatoire lorsque l'unité devient SAC. */
    @DecimalMin(value = "0.001", message = "Le poids du conditionnement doit être positif")
    private BigDecimal packageWeightKg;

    @DecimalMin(value = "0.001", message = "Le prix unitaire doit être supérieur à zéro")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00", message = "Le taux de taxe ne peut pas être négatif")
    private BigDecimal taxRate;

    private LocalDate effectiveFrom;

    private Boolean active;
}
