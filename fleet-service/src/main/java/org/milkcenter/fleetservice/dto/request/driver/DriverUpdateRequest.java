package org.milkcenter.fleetservice.dto.request.driver;

import lombok.*;
import jakarta.validation.constraints.*;


import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverUpdateRequest {

    @NotNull(message = "L'identifiant de l'utilisateur est obligatoire")
    private Long userId;


    @NotNull(message = "Le Numero du licence est obligatoire")
    private String licenseNumber ;

    @NotNull(message = "Le salaire est obligatoire")
    @DecimalMin(value = "400.0", inclusive = true, message = "Le salaire doit être supérieure à 400 dt")
    private BigDecimal salary ;
}
