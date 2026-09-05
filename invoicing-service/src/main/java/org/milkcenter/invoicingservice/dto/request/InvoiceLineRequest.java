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

    /**
     * Pour FEED_SALE : nom du produit configuré.
     * Pour MILK_PURCHASE : peut être ignoré par le service.
     */
    @NotBlank(message = "La description de la ligne est obligatoire")
    @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères")
    private String description;

    /**
     * Exemple : SAC, KG ou LITRE.
     * Pour FEED_SALE, cette valeur aide à rechercher la configuration.
     */
    @Size(max = 30, message = "L'unité ne doit pas dépasser 30 caractères")
    private String unit;

    /** Nombre de sacs, kilogrammes ou litres selon l'unité. */
    @NotNull(message = "La quantité est obligatoire")
    @DecimalMin(value = "0.001", message = "La quantité doit être supérieure à zéro")
    private BigDecimal quantity;

    /** Poids du sac si l'unité est SAC, par exemple 25 ou 50 kg. */
    @DecimalMin(value = "0.001", message = "Le poids du conditionnement doit être positif")
    private BigDecimal packageWeightKg;

    /**
     * Facultatif dans la requête : le service le récupère depuis
     * PricingConfiguration.
     */
    @DecimalMin(value = "0.001", message = "Le prix unitaire doit être supérieur à zéro")
    private BigDecimal unitPrice;

    /**
     * Facultatif dans la requête : le service le récupère depuis
     * PricingConfiguration.
     */
    @DecimalMin(value = "0.00", message = "Le taux de taxe ne peut pas être négatif")
    private BigDecimal taxRate;
}
