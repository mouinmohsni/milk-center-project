package org.milkcenter.fleetservice.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;


import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverUpdateRequest {

    @NotNull(message = "Le Numero du licence est obligatoire")
    private String licenseNumber ;

    @DecimalMin(value = "400.0", inclusive = true, message = "Le salaire doit être supérieure à 400 dt")
    private BigDecimal salary ;
}
