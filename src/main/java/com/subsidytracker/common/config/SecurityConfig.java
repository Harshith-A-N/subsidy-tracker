package com.subsidytracker.common.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.subsidytracker.security.filter.JwtAuthenticationFilter;
import com.subsidytracker.security.service.CustomUserDetailsService;

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
     * - Disbursement plan management (POST, PUT, DELETE) restricted to ADMIN.
     * - Compliance milestone actions (create/complete) restricted to officers + ADMIN.
     * - Compliance milestone listings (GET) restricted to officers + ADMIN.
     * - Disbursement schedule generation restricted to FINANCE_APPROVER + ADMIN.
     * - Disbursement schedule reads restricted to officers + ADMIN (interim —
     *   see TODO below on per-beneficiary ownership scoping).
     * - Analytics and downloadable reports restricted to
     *   DISTRICT_OFFICER, FINANCE_APPROVER, ADMIN.
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
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Public — registration and login
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Public — static portal page itself and static assets
                .requestMatchers("/portal/**", "/favicon.ico", "/error").permitAll()

                // Public — mock external integration endpoints
                .requestMatchers("/mock/**").permitAll()

                // Treasury integration endpoint — FINANCE_APPROVER and ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/integrations/treasury/**")
                    .hasAnyRole("FINANCE_APPROVER", "ADMIN")

                // Scheme browsing is public
                .requestMatchers(HttpMethod.GET, "/api/v1/schemes/**").permitAll()

                // Scheme management — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/schemes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/schemes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/schemes/**").hasRole("ADMIN")

                // Scheme slab and regional budget configuration — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/schemes/*/slabs").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/schemes/*/regional-budgets").hasRole("ADMIN")

                // Beneficiary self-service endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/beneficiaries").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.GET, "/api/v1/beneficiaries/me").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.PUT, "/api/v1/beneficiaries/*").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.POST, "/api/v1/applications").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.GET, "/api/v1/applications/my-applications").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/submit").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/documents").hasRole("BENEFICIARY")

                // Officer/Admin oversight application status listing
                .requestMatchers(HttpMethod.GET, "/api/v1/applications/status/*")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // Disbursement plan configuration — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/disbursement/plans/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/disbursement/plans/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/disbursement/plans/**").hasRole("ADMIN")

                // Compliance milestone creation (ADMIN) and completion (field/district officers & admin)
                .requestMatchers(HttpMethod.POST, "/api/disbursement/compliance/application/*")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/disbursement/compliance/*/complete")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "ADMIN")

                // Compliance milestone listings: pending/overdue
                .requestMatchers(HttpMethod.GET, "/api/disbursement/compliance/pending")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/disbursement/compliance/overdue")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // Per-application milestone reads
                .requestMatchers(HttpMethod.GET, "/api/disbursement/compliance/application/**")
                    .authenticated()

                // Disbursement schedule generation and release
                .requestMatchers(HttpMethod.POST, "/api/disbursement/schedules/generate/*")
                    .hasAnyRole("FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/disbursement/schedules/*/release")
                    .hasAnyRole("FINANCE_APPROVER", "ADMIN")

                // Per-application schedule reads
                .requestMatchers(HttpMethod.GET, "/api/disbursement/schedules/**")
                    .authenticated()

                // Analytics and downloadable reports
                .requestMatchers(HttpMethod.GET, "/api/v1/analytics/**")
                    .hasAnyRole("DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reports/**")
                    .hasAnyRole("DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // Verification actions — officers only
                .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/verify")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER")

                // Document verification — Field Officer only (they're the ones who
                // actually check KYC documents; see DocumentService.verifyDocument).
                // ADMIN also passes via DocumentService's own role check.
                .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/documents/*/verify")
                    .hasAnyRole("FIELD_OFFICER", "ADMIN")

                // Manual eligibility recalculation — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/calculate-eligibility")
                    .hasRole("ADMIN")

                // User listing (Admin "All Users" page) — ADMIN only.
                .requestMatchers(HttpMethod.GET, "/api/v1/users")
                    .hasRole("ADMIN")

                // Officer registration approval management — ADMIN only
                .requestMatchers("/api/v1/admin/officer-registration-requests/**")
                    .hasRole("ADMIN")

                // Everything else requires authentication (any role)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
            "https://digital-subsidy-platform.web.app",
            "https://digital-subsidy-platform.firebaseapp.com",
            "http://localhost:8080",
            "http://127.0.0.1:8080"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}