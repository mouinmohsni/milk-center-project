package org.milkcenter.identityservice.controller;

import lombok.RequiredArgsConstructor;
import org.milkcenter.identityservice.dto.request.RoleUpdateRequest;
import org.milkcenter.identityservice.dto.request.UserUpdateRequest;
import org.milkcenter.identityservice.dto.response.UserResponse;

import org.milkcenter.identityservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


import java.util.List;

@RestController
@RequestMapping("/api/users" )
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest userDetails) {

        return ResponseEntity.ok(userService.updateUser(id, userDetails));
    }

    // Suppression logique : l'utilisateur est désactivé,
// mais reste présent dans la base.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteUser(
            @PathVariable Long id
    ) {
        userService.softDeleteUser(id);

        return ResponseEntity.noContent().build();
    }

    // Suppression physique définitive.
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUser(
            @PathVariable Long id
    ) {
        userService.hardDeleteUser(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest userDetails
    ) {
        return ResponseEntity.ok(
                userService.updateUserRole(id, userDetails)
        );
    }


}
