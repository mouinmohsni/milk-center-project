package org.milkcenter.identityservice.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.milkcenter.identityservice.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpdateRequest {

    @NotNull(message = "Le nouveau role est obligatoire")
    private Role role ;
}
