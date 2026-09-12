package org.milkcenter.identityservice.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakRoleRepresentation {

    private String id;
    private String name;
    private String description;
}
