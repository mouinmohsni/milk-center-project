package org.milkcenter.identityservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakCredentialRequest {

    private String type;
    private String value;
    private boolean temporary;
}
