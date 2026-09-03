package org.milkcenter.invoicingservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceUpdateRequest {

    @FutureOrPresent(message = "La date d'échéance ne peut pas être passée")
    private LocalDate dueDate;

    @Size(max = 500, message = "Les notes ne doivent pas dépasser 500 caractères")
    private String notes;

    /**
     * Permet de remplacer les lignes avant l'émission de la facture.
     * Le service recalculera tous les montants.
     */
    @Valid
    @Size(max = 100, message = "Une facture ne peut pas contenir plus de 100 lignes")
    private List<InvoiceLineRequest> lines;
}
