package com.subsidytracker.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Public — registration and login
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Public — static portal page itself. The page's JS calls the
                // /api/v1/** endpoints below with a Bearer token, so the API
                // calls are still authenticated even though the page loads
                // without one.
                .requestMatchers("/portal/**").permitAll()

                // Scheme browsing is public — a prospective beneficiary should
                // be able to see what schemes exist before registering. Only
                // GET is public; create/update/delete stay ADMIN-only below.
                // Nothing in SchemeResponseDto is sensitive.
                .requestMatchers(HttpMethod.GET, "/api/v1/schemes/**").permitAll()

                // Scheme management — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/schemes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/schemes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/schemes/**").hasRole("ADMIN")

                // Disbursement plan configuration — ADMIN only. Previously
                // unrestricted (fell under the generic authenticated() rule
                // below), so any logged-in user could create/edit/delete a
                // scheme's disbursement plan.
                .requestMatchers(HttpMethod.POST, "/api/disbursement/plans/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/disbursement/plans/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/disbursement/plans/**").hasRole("ADMIN")

                // Compliance milestone actions (create, complete/release funds) —
                // officers and admin only. Previously unrestricted, so a
                // Beneficiary could call PUT /complete directly and release
                // their own funds without any officer sign-off.
                .requestMatchers(HttpMethod.POST, "/api/disbursement/compliance/**")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/disbursement/compliance/**")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // Compliance milestone listings: pending/overdue are system-wide
                // views across all beneficiaries — officers and admin only.
                .requestMatchers(HttpMethod.GET, "/api/disbursement/compliance/pending").
                    hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/disbursement/compliance/overdue")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // Per-application milestone reads: open to any authenticated role.
                // BENEFICIARY is allowed through here because
                // ComplianceMilestoneController.assertCanViewApplication enforces
                // that a beneficiary can only see their own application's data.
                .requestMatchers(HttpMethod.GET, "/api/disbursement/compliance/application/**")
                    .authenticated()

                // Disbursement schedule generation is an administrative/finance
                // action (triggers the actual fund-release plan for an
                // application) — not something a Beneficiary should invoke.
                .requestMatchers(HttpMethod.POST, "/api/disbursement/schedules/**")
                    .hasAnyRole("FINANCE_APPROVER", "ADMIN")

                // Per-application schedule reads: open to any authenticated role.
                // BENEFICIARY is allowed through here because
                // DisbursementController.assertCanViewApplication enforces that
                // a beneficiary can only see their own application's schedule.
                .requestMatchers(HttpMethod.GET, "/api/disbursement/schedules/**")
                    .authenticated()

                // Analytics and downloadable reports expose scheme/region-wide
                // budget and compliance data — restricted to roles with oversight
                // responsibility, matching the project guide's roles table.
                .requestMatchers(HttpMethod.GET, "/api/v1/analytics/**")
                    .hasAnyRole("DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reports/**")
                    .hasAnyRole("DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // Verification actions — officers only
                .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/verify")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER")

                // Document verification — officers only
                .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/documents/*/verify")
                    .hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER")

                // Manual eligibility recalculation — ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/calculate-eligibility")
                    .hasRole("ADMIN")

                // User listing (Admin "All Users" page) — ADMIN only.
                // Note: this exact-path matcher does NOT catch /api/v1/users/me,
                // which any authenticated user can hit via the catch-all below.
                .requestMatchers(HttpMethod.GET, "/api/v1/users")
                    .hasRole("ADMIN")

                // Everything else requires authentication (any role)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}