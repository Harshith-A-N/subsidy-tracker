package com.subsidytracker.security.controller;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.security.dto.OfficerRegistrationResponseDto;
import com.subsidytracker.security.dto.OfficerRejectRequestDto;
import com.subsidytracker.security.service.OfficerRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/officer-registration-requests")
public class AdminOfficerRegistrationController {

    private final OfficerRegistrationService registrationService;
    private final UserRepository userRepository;

    public AdminOfficerRegistrationController(OfficerRegistrationService registrationService,
                                               UserRepository userRepository) {
        this.registrationService = registrationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<OfficerRegistrationResponseDto>> getPendingRequests() {
        return ResponseEntity.ok(registrationService.getPendingRequests());
    }

    @GetMapping("/all")
    public ResponseEntity<List<OfficerRegistrationResponseDto>> getAllRequests() {
        return ResponseEntity.ok(registrationService.getAllRequests());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<OfficerRegistrationResponseDto> approveRequest(@PathVariable Long id) {
        User admin = getAuthenticatedAdmin();
        return ResponseEntity.ok(registrationService.approveRequest(id, admin));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OfficerRegistrationResponseDto> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) OfficerRejectRequestDto body) {
        User admin = getAuthenticatedAdmin();
        String reason = body != null ? body.getRejectionReason() : null;
        return ResponseEntity.ok(registrationService.rejectRequest(id, reason, admin));
    }

    private User getAuthenticatedAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}
