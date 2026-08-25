package org.milkcenter.identityservice.service;

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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository ;
    private final PasswordEncoder passwordEncoder; // Injecté automatiquement via Spring Security


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userDetail = userRepository.findByUsername(username);

        // Convertir notre User en UserDetails de Spring Security
        return userDetail.map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        user.getAuthorities() // Assurez-vous que votre User model a getAuthorities()
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + username));
    }


    public User registerUser(User user) {
        // 1. Hacher le mot de passe avant la sauvegarde
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Sauvegarder l'utilisateur en base
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // --- UPDATE (Mise à jour) ---
    public User updateUser(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    // Mettre à jour uniquement les champs fournis (non null)
                    if (userDetails.getFirstName() != null) {
                        existingUser.setFirstName(userDetails.getFirstName());
                    }
                    if (userDetails.getLastName() != null) {
                        existingUser.setLastName(userDetails.getLastName());
                    }
                    if (userDetails.getPhoneNumber() != null) {
                        existingUser.setPhoneNumber(userDetails.getPhoneNumber());
                    }
                    // NE JAMAIS écraser : role, username, isEnabled, createdAt
                    // Ils gardent automatiquement leurs valeurs existantes

                    // Le mot de passe n'est mis à jour que s'il est fourni
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                    }

                    return userRepository.save(existingUser);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilisateur non trouvé avec l'id " + id
                ));
    }


    // --- DELETE (Suppression) ---
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // --- LOGIN LOGIC (Vérification des identifiants) ---
    public boolean verifyLogin(String username, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // On compare le mot de passe saisi avec le hash en base
            return passwordEncoder.matches(rawPassword, user.getPassword());
        }
        return false;
    }






}
