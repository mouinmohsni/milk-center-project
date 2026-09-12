package org.milkcenter.identityservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Email(message = "L'adresse email est invalide")
    @Size(max = 150, message = "L'adresse email ne doit pas dépasser 150 caractères")
    private String email;

    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
    private String firstName;

    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String lastName;

    private String phoneNumber;

    /** Conservé temporairement pour l’ancien flux de login. */
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;
}
