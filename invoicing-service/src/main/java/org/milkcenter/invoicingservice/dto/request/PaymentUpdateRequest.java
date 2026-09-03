package org.milkcenter.invoicingservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentUpdateRequest {

    @DecimalMin(
            value = "0.01",
            message = "Le montant doit être supérieur à zéro"
    )
    private BigDecimal amount;

    @PastOrPresent(
            message = "La date du paiement ne peut pas être dans le futur"
    )
    private LocalDate paymentDate;

    @Size(
            max = 30,
            message = "La méthode de paiement ne doit pas dépasser 30 caractères"
    )
    private String paymentMethod;

    @Size(
            max = 100,
            message = "La référence ne doit pas dépasser 100 caractères"
    )
    private String reference;

    @Size(
            max = 500,
            message = "Les notes ne doivent pas dépasser 500 caractères"
    )
    private String notes;
}
