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
        // Prevent duplicate email registration
        userRepository.findByEmail(request.getEmail())
                .ifPresent(existing -> {
                    throw new InvalidOperationException(
                            "An account with this email already exists.");
                });

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.BENEFICIARY);
        // region intentionally left null for beneficiaries

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getEmail());

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
        // This throws BadCredentialsException if authentication fails
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // If we reach here, authentication succeeded — fetch the user for the response
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidOperationException(
                        "User not found after successful authentication."));

        String token = jwtService.generateToken(user.getEmail());

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
