package com.subsidytracker.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary security configuration that permits ALL requests without authentication.
 *
 * WHY THIS EXISTS:
 * The pom.xml includes spring-boot-starter-security. Without this config,
 * Spring Security blocks every endpoint with a login page, making it impossible
 * to test APIs via Postman.
 *
 * Module 5 (Security) will replace this with proper JWT-based authentication later.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for REST APIs (stateless, no browser forms)
            .csrf(csrf -> csrf.disable())
            // Permit all requests — no authentication required during development
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
