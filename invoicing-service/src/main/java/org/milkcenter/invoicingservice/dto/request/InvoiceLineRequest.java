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

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineRequest {

    @NotBlank(message = "La description de la ligne est obligatoire")
    @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères")
    private String description;

    @Size(max = 30, message = "L'unité ne doit pas dépasser 30 caractères")
    private String unit;

    @NotNull(message = "La quantité est obligatoire")
    @DecimalMin(value = "0.001", message = "La quantité doit être supérieure à zéro")
    private BigDecimal quantity;

    @NotNull(message = "Le prix unitaire est obligatoire")
    @DecimalMin(value = "0.00", inclusive = false, message = "Le prix unitaire doit être supérieur à zéro")
    private BigDecimal unitPrice;

    @NotNull(message = "Le taux de taxe est obligatoire")
    @DecimalMin(value = "0.00", message = "Le taux de taxe ne peut pas être négatif")
    private BigDecimal taxRate;
}
