package com.subsidytracker.security.controller;

import com.subsidytracker.security.dto.AuthResponseDto;
import com.subsidytracker.security.dto.LoginRequestDto;
import com.subsidytracker.security.dto.RegisterRequestDto;
import com.subsidytracker.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Public endpoint — registers a new beneficiary account.
     * Role is server-assigned; the caller only provides fullName, email, password.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.registerBeneficiary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Public endpoint — authenticates any user (beneficiary, officer, admin).
     * Returns the user's identity and role on success.
     * Spring Security throws 401 on bad credentials before this method returns.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
