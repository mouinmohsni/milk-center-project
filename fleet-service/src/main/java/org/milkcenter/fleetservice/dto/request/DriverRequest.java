package org.milkcenter.fleetservice.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;
import org.milkcenter.fleetservice.enums.DriverStatus;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverRequest {


    @NotNull(message = "Le userId est obligatoire")
    private Long userId ;

    @NotBlank(message = "Le Numero du licence est obligatoire")
    private String licenseNumber ;

    @DecimalMin(value = "400.0", inclusive = true, message = "Le salaire doit être supérieure à 400 dt")
    private BigDecimal salary ;

    private DriverStatus status;

}
