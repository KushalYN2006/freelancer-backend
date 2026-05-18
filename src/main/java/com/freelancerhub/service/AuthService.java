package com.freelancerhub.service;

import com.freelancerhub.model.User;
import com.freelancerhub.repository.UserRepository;
import com.freelancerhub.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Autowired
    private UserRepository userRepository;   // ← talks to MySQL

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // REGISTER: Save user to MySQL
    public Map<String, Object> register(User user) {
        validateEmail(user.getEmail());

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        // Hash password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);  // INSERT INTO Users ...

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("userId", saved.getUserId());
        return response;
    }

    // LOGIN: Verify credentials, return JWT token
    public Map<String, Object> login(String email, String password) {
        validateEmail(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole());
        response.put("userId", user.getUserId());
        response.put("name", user.getName());
        return response;
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new RuntimeException("Please enter a valid email address");
        }
    }
}
