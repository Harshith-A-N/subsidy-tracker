package com.subsidytracker.eligibility.controller;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.eligibility.dto.VerificationRequestDto;
import com.subsidytracker.eligibility.dto.VerificationResponseDto;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
public class VerificationController {

    private final VerificationService verificationService;
    private final UserRepository userRepository;

    public VerificationController(VerificationService verificationService,
                                  UserRepository userRepository) {
        this.verificationService = verificationService;
        this.userRepository = userRepository;
    }

    @PatchMapping("/{applicationId}/verify")
    public ResponseEntity<VerificationResponseDto> verify(@PathVariable Long applicationId,
                                                          @RequestBody VerificationRequestDto request,
                                                          Authentication authentication) {
        long officerId = resolveUserId(authentication);
        return ResponseEntity.ok(verificationService.processVerification(applicationId, request, officerId));
    }

    @PatchMapping("/{applicationId}/resume-verification")
    public ResponseEntity<VerificationResponseDto> resume(@PathVariable Long applicationId) {
        return ResponseEntity.ok(verificationService.resumeAfterReVerification(applicationId));
    }

    /**
     * Resolves the current user's database ID from the Authentication principal.
     * The principal name is the email (set by CustomUserDetailsService).
     */
    private long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));
        return user.getId();
    }
}