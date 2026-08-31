package org.milkcenter.identityservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.identityservice.dto.request.UserLoginRequest;
import org.milkcenter.identityservice.dto.request.UserRegisterRequest;
import org.milkcenter.identityservice.dto.response.UserResponse;
import org.milkcenter.identityservice.model.User;
import org.milkcenter.identityservice.service.JwtService;
import org.milkcenter.identityservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;




import java.util.Map;

@RestController
@RequestMapping("/api/auth" )
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody UserLoginRequest loginRequest
    ) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            User user = userService
                    .getUserByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new IllegalStateException(
                            "Utilisateur introuvable après authentification"
                    ));

            String token = jwtService.generateToken(
                    user.getUsername(),
                    user.getRole().name()
            );

            return ResponseEntity.ok(Map.of("token", token));

        } catch (AuthenticationException exception) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Identifiants incorrects ou compte désactivé.");
        }
    }
}
