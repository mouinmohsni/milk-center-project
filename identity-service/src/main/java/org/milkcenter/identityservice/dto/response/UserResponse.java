package org.milkcenter.identityservice.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.milkcenter.identityservice.enums.Role;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private boolean enabled;
    private Date createdAt;
    private Date updatedAt;
}
