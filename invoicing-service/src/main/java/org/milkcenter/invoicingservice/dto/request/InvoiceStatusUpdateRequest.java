package org.milkcenter.invoicingservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.milkcenter.invoicingservice.enums.InvoiceStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceStatusUpdateRequest {

    @NotNull(message = "Le nouveau statut est obligatoire")
    private InvoiceStatus status;

    @Size(max = 500, message = "La raison ne doit pas dépasser 500 caractères")
    private String reason;
}
