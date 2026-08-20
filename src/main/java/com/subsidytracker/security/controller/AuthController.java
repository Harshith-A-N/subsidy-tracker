package com.subsidytracker.security.controller;

import com.subsidytracker.security.dto.AuthResponseDto;
import com.subsidytracker.security.dto.LoginRequestDto;
import com.subsidytracker.security.dto.OfficerRegistrationRequestDto;
import com.subsidytracker.security.dto.OfficerRegistrationResponseDto;
import com.subsidytracker.security.dto.RegisterRequestDto;
import com.subsidytracker.security.service.AuthService;
import com.subsidytracker.security.service.OfficerRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final OfficerRegistrationService officerRegistrationService;

    public AuthController(AuthService authService,
                          OfficerRegistrationService officerRegistrationService) {
        this.authService = authService;
        this.officerRegistrationService = officerRegistrationService;
    }

    /**
     * Public endpoint — registers a new beneficiary account.
     * Role is server-assigned; the caller only provides fullName, email, password.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.registerBeneficiary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Public endpoint — submits a request for officer registration (FIELD_OFFICER, DISTRICT_OFFICER, FINANCE_APPROVER).
     * No user account is created until an Admin approves the request.
     */
    @PostMapping("/officer-register")
    public ResponseEntity<OfficerRegistrationResponseDto> registerOfficer(@RequestBody OfficerRegistrationRequestDto request) {
        OfficerRegistrationResponseDto response = officerRegistrationService.submitRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Public endpoint — authenticates any user (beneficiary, officer, admin).
     * Returns the user's identity and role on success.
     * Spring Security throws 401 on bad credentials before this method returns.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
