package com.freelancerhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()   // public
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/projects/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**").permitAll()
                .requestMatchers("/api/reviews", "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/projects").permitAll()
                .requestMatchers("/api/bids", "/api/bids/**").permitAll()
                .requestMatchers("/api/contracts", "/api/contracts/**").permitAll()
                .requestMatchers("/api/messages", "/api/messages/**").permitAll()
                .requestMatchers("/api/notifications", "/api/notifications/**").permitAll()
                .requestMatchers("/api/users", "/api/users/**").permitAll()
                .requestMatchers("/api/freelancer-profiles", "/api/freelancer-profiles/**").permitAll()
                .requestMatchers("/api/conversation-invitations", "/api/conversation-invitations/**").permitAll()
                .anyRequest().authenticated()                  // rest need JWT
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // hashes passwords before DB storage
    }
}
