package com.subsidytracker.common.config;

import com.subsidytracker.security.filter.JwtAuthenticationFilter;
import com.subsidytracker.security.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Security filter chain with role-based URL protection and JWT filter.
     *
     * - Auth endpoints (register/login) are fully public.
     * - Scheme management (POST, PUT, DELETE) restricted to ADMIN.
     * - Verification actions restricted to officers.
     * - Document verification restricted to officers.
     * - Manual eligibility recalculation restricted to ADMIN.
     * - All other endpoints require authentication (any role).
     * - CSRF disabled: pure REST API, no browser form submissions.
     * - Stateless sessions: token carried in Authorization header.
     * - JwtAuthenticationFilter added before UsernamePasswordAuthenticationFilter.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Public — registration and login
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Scheme management — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/schemes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/schemes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/schemes/**").hasRole("ADMIN")

                // Verification actions — officers only
                .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/verify")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER")

                // Document verification — officers only
                .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/documents/*/verify")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER")

                // Manual eligibility recalculation — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/calculate-eligibility")
                    .hasRole("ADMIN")

                // Everything else requires authentication (any role)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
