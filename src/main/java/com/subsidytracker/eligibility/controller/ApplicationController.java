package com.subsidytracker.eligibility.controller;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.eligibility.dto.ApplicationRequestDto;
import com.subsidytracker.eligibility.dto.ApplicationResponseDto;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserRepository userRepository;

    public ApplicationController(ApplicationService applicationService,
                                 UserRepository userRepository) {
        this.applicationService = applicationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponseDto> create(@RequestBody ApplicationRequestDto request,
                                                          Authentication authentication) {
        long userId = resolveUserId(authentication);
        ApplicationResponseDto created = applicationService.createApplication(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns only applications belonging to the authenticated beneficiary.
     */
    @GetMapping("/my-applications")
    public ResponseEntity<List<ApplicationResponseDto>> getMyApplications(Authentication authentication) {
        long userId = resolveUserId(authentication);
        return ResponseEntity.ok(applicationService.getMyApplications(userId));
    }

    /**
     * Formally submits a DRAFT application for eligibility evaluation.
     * Documents must be uploaded before calling this endpoint.
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApplicationResponseDto> submit(@PathVariable Long id,
                                                          Authentication authentication) {
        long userId = resolveUserId(authentication);
        ApplicationResponseDto result = applicationService.submitApplication(id, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Returns a single application by ID.
     * Beneficiaries can only access their own applications.
     * Officers and admins can access any application.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> getById(@PathVariable Long id,
                                                          Authentication authentication) {
        long userId = resolveUserId(authentication);
        return ResponseEntity.ok(applicationService.getApplicationById(id, userId));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponseDto>> getAll() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApplicationResponseDto>> getByStatus(@PathVariable ApplicationStatus status) {
        return ResponseEntity.ok(applicationService.getApplicationsByStatus(status));
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