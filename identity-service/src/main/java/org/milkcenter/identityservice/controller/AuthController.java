package org.milkcenter.identityservice.controller;


import lombok.RequiredArgsConstructor;
import org.milkcenter.identityservice.model.User;
import org.milkcenter.identityservice.service.JwtService;
import org.milkcenter.identityservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth" )
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        return ResponseEntity.ok(userService.registerUser(user));
    }

    private final JwtService jwtService; // Ajoutez l'injection

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (userService.verifyLogin(username, password)) {
            // On récupère l'utilisateur pour avoir son rôle
            User user = userService.getUserByUsername(username).get();
            // On génère le token
            String token = jwtService.generateToken(username, user.getRole().name());

            // On renvoie le token dans un objet JSON
            return ResponseEntity.ok(Map.of("token", token));
        } else {
            return ResponseEntity.status(401).body("Identifiants incorrects.");
        }
    }
}
