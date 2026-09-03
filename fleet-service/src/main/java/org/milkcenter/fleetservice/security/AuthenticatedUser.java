package org.milkcenter.fleetservice.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticatedUser {

    private Long userId;
    private String username;
    private String role;
}
