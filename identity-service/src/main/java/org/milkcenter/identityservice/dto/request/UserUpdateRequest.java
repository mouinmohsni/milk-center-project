package org.milkcenter.identityservice.dto.request;



import jakarta.validation.constraints.Size;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    private String firstName;

    private String lastName;

    private String phoneNumber;

    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;
}
