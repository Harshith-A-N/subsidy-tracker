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
                // 1. Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/dashboard/**").permitAll()

                // 2. Beneficiary self-service
                .requestMatchers(HttpMethod.POST, "/api/v1/beneficiaries").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.GET, "/api/v1/beneficiaries/me").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.PUT, "/api/v1/beneficiaries/*").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.POST, "/api/v1/applications").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.GET, "/api/v1/applications/my-applications").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/submit").hasRole("BENEFICIARY")
                .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/documents").hasRole("BENEFICIARY")

                // 3. Officer/Admin oversight read access
                .requestMatchers(HttpMethod.GET, "/api/v1/beneficiaries").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/applications").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/applications/status/*").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // 4. Shared application/document read access
                .requestMatchers(HttpMethod.GET, "/api/v1/applications/*").hasAnyRole("BENEFICIARY", "FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/applications/*/documents").hasAnyRole("BENEFICIARY", "FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // 5. Document & application verification (officers only)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/documents/*/verify").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/verify").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/resume-verification").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER")

                // 6. Manual eligibility recalculation (ADMIN only)
                .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/calculate-eligibility").hasRole("ADMIN")

                // 7. Scheme management (ADMIN write, authenticated read)
                .requestMatchers(HttpMethod.POST, "/api/v1/schemes").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/schemes/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/schemes/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/schemes/*/slabs").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/schemes/*/regional-budgets").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/schemes").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/schemes/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/schemes/*/slabs").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/schemes/*/regional-budgets").authenticated()

                // 8. Disbursement plan management
                .requestMatchers(HttpMethod.POST, "/api/disbursement/plans").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/disbursement/plans/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/disbursement/plans/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/disbursement/plans/*").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/disbursement/plans/scheme/*").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // 9. Schedule generation & viewing
                .requestMatchers(HttpMethod.POST, "/api/disbursement/schedules/generate/*").hasAnyRole("FINANCE_APPROVER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/disbursement/schedules/application/*").hasAnyRole("BENEFICIARY", "FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // 10. Compliance milestones
                .requestMatchers(HttpMethod.POST, "/api/disbursement/compliance/application/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/disbursement/compliance/*/complete").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER")
                .requestMatchers(HttpMethod.GET, "/api/disbursement/compliance/pending").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/disbursement/compliance/overdue").hasAnyRole("FIELD_OFFICER", "DISTRICT_OFFICER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/disbursement/compliance/application/*").hasAnyRole("BENEFICIARY", "FIELD_OFFICER", "DISTRICT_OFFICER", "FINANCE_APPROVER", "ADMIN")

                // 11. Analytics / Dashboard / Reports (oversight)
                .requestMatchers("/api/v1/analytics/**").hasAnyRole("ADMIN", "FINANCE_APPROVER", "DISTRICT_OFFICER")
                .requestMatchers("/api/v1/dashboard/**").hasAnyRole("ADMIN", "FINANCE_APPROVER", "DISTRICT_OFFICER")
                .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "FINANCE_APPROVER", "DISTRICT_OFFICER")

                // 12. Fallback
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
