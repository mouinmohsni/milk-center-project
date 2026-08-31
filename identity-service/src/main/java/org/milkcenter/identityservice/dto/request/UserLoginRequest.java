package org.milkcenter.identityservice.dto.request;



import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginRequest {

    @NotBlank(message = "Le username est obligatoire")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
