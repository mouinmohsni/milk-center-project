package org.milkcenter.identityservice.repository;

import org.milkcenter.identityservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Recherche un utilisateur par son username (Retourne un Optional pour éviter les NullPointerException)
    Optional<User> findByUsername(String username);

    // Vérifie si un email existe déjà dans la base de données
    Optional<User> findByPhoneNumber(String phoneNumber);
}