package org.milkcenter.invoicingservice.dto.request;



import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class PricingConfigurationCreateRequest {

    @NotNull(message = "Le type de facture est obligatoire")
    private InvoiceType invoiceType;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 150, message = "Le nom du produit ne doit pas dépasser 150 caractères")
    private String productName;

    @NotNull(message = "L'unité de vente est obligatoire")
    private SaleUnit saleUnit;

    /** Obligatoire pour SAC, null pour LITRE ou KG. */
    @DecimalMin(value = "0.001", message = "Le poids du conditionnement doit être positif")
    private BigDecimal packageWeightKg;

    @NotNull(message = "Le prix unitaire est obligatoire")
    @DecimalMin(value = "0.001", message = "Le prix unitaire doit être supérieur à zéro")
    private BigDecimal unitPrice;

    @NotNull(message = "Le taux de taxe est obligatoire")
    @DecimalMin(value = "0.00", message = "Le taux de taxe ne peut pas être négatif")
    private BigDecimal taxRate;

    @NotNull(message = "La date d'effet est obligatoire")
    private LocalDate effectiveFrom;

    /** Par défaut, la configuration créée est active. */
    @Builder.Default
    private Boolean active = true;
}
