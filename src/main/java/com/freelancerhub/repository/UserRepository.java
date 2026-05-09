package com.freelancerhub.repository;

import com.freelancerhub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository gives you free SQL: save(), findById(), findAll(), delete()
public interface UserRepository extends JpaRepository<User, Integer> {

    // Custom query: SELECT * FROM Users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Check if email already exists
    boolean existsByEmail(String email);
}