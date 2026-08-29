package com.subsidytracker.security.service;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.security.dto.AuthResponseDto;
import com.subsidytracker.security.dto.LoginRequestDto;
import com.subsidytracker.security.dto.RegisterRequestDto;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new beneficiary account.
     * Role is always BENEFICIARY — callers cannot choose their own role.
     * Region is left null since beneficiaries have no region assignment.
     */
    @Transactional
    public AuthResponseDto registerBeneficiary(RegisterRequestDto request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        // Prevent duplicate email registration
        userRepository.findByEmail(email)
                .ifPresent(existing -> {
                    throw new InvalidOperationException(
                            "An account with this email already exists.");
                });

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.BENEFICIARY);
        // region intentionally left null for beneficiaries

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getEmail(), saved.getRole().name());

        return new AuthResponseDto(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getRole(),
                token,
                "Registration successful."
        );
    }

    /**
     * Authenticates a user (any role) via Spring Security's AuthenticationManager.
     * On success, returns the user's identity, role, and JWT token.
     * On failure, Spring Security throws AuthenticationException (handled by default).
     */
    public AuthResponseDto login(LoginRequestDto request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";

        // 1. Priority 1: Check blank fields (server-side safeguard)
        if (email.isEmpty()) {
            throw new InvalidOperationException("Please enter your email address.");
        }
        if (password.isEmpty()) {
            throw new InvalidOperationException("Please enter your password.");
        }

        // 2. Priority 2: Check if email exists in database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOperationException("Invalid email: No account found with this email."));

        // 3. Priority 3: Check if password is correct
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidOperationException("Invalid password: The password entered is incorrect.");
        }

        // 4. Priority 4: Check if role matches account role
        if (request.getRole() != null && user.getRole() != request.getRole()) {
            throw new InvalidOperationException("Role mismatch: This account is registered as " + user.getRole() + ".");
        }

        // Authenticate Spring Security context if needed
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (Exception ignored) {
            // Already validated against database and password encoder
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                token,
                "Login successful."
        );
    }
}
