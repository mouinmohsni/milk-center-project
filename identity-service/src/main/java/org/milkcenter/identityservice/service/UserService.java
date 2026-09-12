package org.milkcenter.identityservice.service;


import lombok.RequiredArgsConstructor;
import org.milkcenter.identityservice.client.KeycloakAdminClient;
import org.milkcenter.identityservice.client.KeycloakCreateUserRequest;
import org.milkcenter.identityservice.client.KeycloakCredentialRequest;
import org.milkcenter.identityservice.dto.request.RoleUpdateRequest;
import org.milkcenter.identityservice.dto.request.UserRegisterRequest;
import org.milkcenter.identityservice.dto.request.UserUpdateRequest;
import org.milkcenter.identityservice.dto.response.UserResponse;
import org.milkcenter.identityservice.enums.Role;
import org.milkcenter.identityservice.model.User;
import org.milkcenter.identityservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAdminClient keycloakAdminClient;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé : " + username
                ));
    }

    public UserResponse registerUser(UserRegisterRequest request) {
        validateRegistrationUniqueness(request);

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setPhoneNumber(request.getPhoneNumber());
        newUser.setRole(Role.FARMER);
        newUser.setEnabled(true);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        // Première sauvegarde afin d'obtenir l'identifiant local utilisé
        // comme attribut userId dans Keycloak.
        User savedUser = userRepository.save(newUser);

        KeycloakCreateUserRequest keycloakRequest =
                KeycloakCreateUserRequest.builder()
                        .username(savedUser.getUsername())
                        .email(savedUser.getEmail())
                        .firstName(savedUser.getFirstName())
                        .lastName(savedUser.getLastName())
                        .enabled(savedUser.isEnabled())
                        // Les utilisateurs de l'application n'utilisent pas
                        // de procédure de vérification d'e-mail.
                        .emailVerified(true)
                        .attributes(Map.of(
                                "userId",
                                List.of(String.valueOf(savedUser.getId()))
                        ))
                        .credentials(List.of(
                                KeycloakCredentialRequest.builder()
                                        .type("password")
                                        .value(request.getPassword())
                                        .temporary(false)
                                        .build()
                        ))
                        .build();

        try {
            String keycloakUserId =
                    keycloakAdminClient.createUser(keycloakRequest);

            keycloakAdminClient.assignRealmRole(
                    keycloakUserId,
                    Role.FARMER.name()
            );

            // Correction essentielle : cette sauvegarde persistait auparavant
            // uniquement en mémoire. Sans elle, keycloak_user_id restait NULL.
            savedUser.setKeycloakUserId(keycloakUserId);
            userRepository.save(savedUser);

        } catch (RuntimeException exception) {
            // Compensation locale si la création ou la configuration Keycloak
            // échoue après la création de l'utilisateur local.
            userRepository.delete(savedUser);

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Impossible de synchroniser l'utilisateur avec Keycloak",
                    exception
            );
        }

        return mapToResponse(savedUser);
    }

    private void validateRegistrationUniqueness(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le username " + request.getUsername() + " est déjà utilisé"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'adresse email " + request.getEmail() + " est déjà utilisée"
            );
        }

        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().isBlank()
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le numéro de téléphone " + request.getPhoneNumber()
                            + " est déjà utilisé"
            );
        }
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Optional<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::mapToResponse);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest userDetails) {
        User existingUser = findUserById(id);

        if (userDetails.getEmail() != null
                && !userDetails.getEmail().isBlank()
                && !userDetails.getEmail().equals(existingUser.getEmail())
                && userRepository.existsByEmail(userDetails.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'adresse email est déjà utilisée"
            );
        }

        if (userDetails.getEmail() != null
                && !userDetails.getEmail().isBlank()) {
            existingUser.setEmail(userDetails.getEmail());
        }
        if (userDetails.getFirstName() != null) {
            existingUser.setFirstName(userDetails.getFirstName());
        }
        if (userDetails.getLastName() != null) {
            existingUser.setLastName(userDetails.getLastName());
        }
        if (userDetails.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(userDetails.getPhoneNumber());
        }
        if (userDetails.getPassword() != null
                && !userDetails.getPassword().isBlank()) {
            existingUser.setPassword(
                    passwordEncoder.encode(userDetails.getPassword())
            );
        }

        return mapToResponse(userRepository.save(existingUser));
    }

    public UserResponse updateUserRole(Long id, RoleUpdateRequest userDetails) {
        User existingUser = findUserById(id);

        if (userDetails.getRole() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le rôle est obligatoire"
            );
        }

        existingUser.setRole(userDetails.getRole());
        return mapToResponse(userRepository.save(existingUser));
    }

    public void hardDeleteUser(Long id) {
        userRepository.delete(findUserById(id));
    }

    public void softDeleteUser(Long id) {
        User user = findUserById(id);
        user.setEnabled(false);
        userRepository.save(user);
    }

    public boolean verifyLogin(String username, String rawPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        return user.isEnabled()
                && passwordEncoder.matches(rawPassword, user.getPassword());
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun utilisateur trouvé avec l'ID : " + id
                ));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
