package org.milkcenter.collectionservice.dto.request;


import lombok.*;
import jakarta.validation.constraints.*;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerProfileRequest {

    @NotNull(message = "Le userId est obligatoire")
    private Long userId;

    @NotBlank(message = "Le nom de la ferme est obligatoire")
    @Size(max = 100, message = "Le nom de la ferme ne doit pas dépasser 100 caractères")
    private String farmName;

    @NotBlank(message = "L'adresse est obligatoire")
    private String address;

    @NotNull(message = "La latitude est obligatoire")
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private Double longitude;

    @NotNull(message = "La taille du troupeau est obligatoire")
    @Min(value = 1, message = "Le troupeau doit avoir au moins 1 animal")
    @Max(value = 10000, message = "Le troupeau ne peut pas dépasser 10 000 animaux")
    private Integer herdSize;
}
