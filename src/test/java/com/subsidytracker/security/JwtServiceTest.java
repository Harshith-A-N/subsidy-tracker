package com.subsidytracker.security;

import com.subsidytracker.common.enums.Role;
import com.subsidytracker.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set secret key (64 hex characters) and 1 hour expiration for testing
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    @Test
    void generateToken_ShouldContainSubjectAndRole() {
        String email = "testuser@example.com";
        String role = Role.BENEFICIARY.name();

        String token = jwtService.generateToken(email, role);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
        assertThat(jwtService.extractRole(token)).isEqualTo(role);
    }

    @Test
    void isTokenValid_ShouldReturnTrueForValidTokenAndMatchingUser() {
        String email = "officer@example.com";
        String token = jwtService.generateToken(email, Role.FIELD_OFFICER.name());

        UserDetails userDetails = new User(
                email,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_FIELD_OFFICER"))
        );

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }
}
