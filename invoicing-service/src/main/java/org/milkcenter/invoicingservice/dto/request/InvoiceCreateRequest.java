package org.milkcenter.invoicingservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.milkcenter.invoicingservice.enums.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceCreateRequest {

    @NotNull(message = "L'identifiant du fermier est obligatoire")
    private Long farmerId;

    @NotNull(message = "Le type de facture est obligatoire")
    private InvoiceType invoiceType;

    @NotNull(message = "Le mois de facturation est obligatoire")
    @Min(value = 1, message = "Le mois doit être compris entre 1 et 12")
    @Max(value = 12, message = "Le mois doit être compris entre 1 et 12")
    private Integer billingMonth;

    @NotNull(message = "L'année de facturation est obligatoire")
    @Min(value = 2000, message = "L'année de facturation est invalide")
    private Integer billingYear;

    /**
     * Date d'émission facultative. Le service peut la définir automatiquement.
     */
    private LocalDate issueDate;

    /** Date limite de paiement, facultative. */
    private LocalDate dueDate;

    /**
     * Taux de taxe général de la facture. Les montants restent calculés par le service.
     */
    @NotNull(message = "Le taux de taxe est obligatoire")
    @Min(value = 0, message = "Le taux de taxe ne peut pas être négatif")
    private BigDecimal taxRate;

    @Size(max = 500, message = "Les notes ne doivent pas dépasser 500 caractères")
    private String notes;

    /**
     * Lignes de nourriture ou ligne de lait.
     * Pour MILK_PURCHASE, le service pourra remplacer la quantité par le total
     * calculé à partir des collections du mois.
     */
    @Valid
    @Size(max = 100, message = "Une facture ne peut pas contenir plus de 100 lignes")
    @Builder.Default
    private List<InvoiceLineRequest> lines = new ArrayList<>();
}
