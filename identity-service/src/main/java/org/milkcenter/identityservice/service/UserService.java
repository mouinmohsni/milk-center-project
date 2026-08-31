package org.milkcenter.identityservice.service;

import org.milkcenter.identityservice.dto.request.RoleUpdateRequest;
import org.milkcenter.identityservice.dto.request.UserRegisterRequest;
import org.milkcenter.identityservice.dto.request.UserUpdateRequest;
import org.milkcenter.identityservice.enums.Role;
import org.milkcenter.identityservice.model.User;
import org.milkcenter.identityservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.server.ResponseStatusException;
import org.milkcenter.identityservice.dto.response.UserResponse;


import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository ;
    private final PasswordEncoder passwordEncoder; // Injecté automatiquement via Spring Security


    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé : " + username
                ));
    }



    public UserResponse registerUser(UserRegisterRequest request) {


        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le username " + request.getUsername()
                            + " est déjà utilisé"
            );
        }

        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().isBlank()
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le numéro de téléphone "
                            + request.getPhoneNumber()
                            + " est déjà utilisé"
            );
        }
        User newUser = new User();

        newUser.setUsername(request.getUsername());
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setPhoneNumber(request.getPhoneNumber());

        // Le rôle de l'inscription publique est défini côté serveur.
        newUser.setRole(Role.FARMER);

        // Le mot de passe doit être encodé avant la sauvegarde.
        newUser.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepository.save(newUser);
        return mapToResponse(savedUser);
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
        User existingUser =findUserById(id);

                    if (userDetails.getFirstName() != null) {
                        existingUser.setFirstName(userDetails.getFirstName());
                    }
                    if (userDetails.getLastName() != null) {
                        existingUser.setLastName(userDetails.getLastName());
                    }
                    if (userDetails.getPhoneNumber() != null) {
                        existingUser.setPhoneNumber(userDetails.getPhoneNumber());
                    }


                    // Le mot de passe n'est mis à jour que s'il est fourni
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                    }

                    User update = userRepository.save(existingUser);
        return mapToResponse(update);
    }

    public UserResponse updateUserRole(Long id, RoleUpdateRequest userDetails) {
        User existingUser =findUserById(id);

        if (userDetails.getRole() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le rôle est obligatoire"
            );
        }

        existingUser.setRole(userDetails.getRole());
        User update = userRepository.save(existingUser);
        return mapToResponse(update);
    }





    // --- DELETE (Suppression) ---
    public void hardDeleteUser(Long id) {
        User user = findUserById(id);

        userRepository.delete(user);
    }

    public void softDeleteUser(Long id) {
        User user = findUserById(id);

        user.setEnabled(false);

        userRepository.save(user);
    }

    // --- LOGIN LOGIC (Vérification des identifiants) ---
    public boolean verifyLogin(String username, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Un compte désactivé ne peut pas se connecter.
        if (!user.isEnabled()) {
            return false;
        }

        // Comparaison du mot de passe saisi avec le mot de passe haché.
        return passwordEncoder.matches(
                rawPassword,
                user.getPassword()
        );
    }



    private User findUserById(Long id ){
        return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Aucun user trouvé avec l'ID : " + id

        ));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
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
