package com.subsidytracker.beneficiary.controller;

import com.subsidytracker.beneficiary.dto.BeneficiaryRequestDto;
import com.subsidytracker.beneficiary.dto.BeneficiaryResponseDto;
import com.subsidytracker.beneficiary.service.BeneficiaryService;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.eligibility.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final UserRepository userRepository;

    public BeneficiaryController(BeneficiaryService beneficiaryService,
                                 UserRepository userRepository) {
        this.beneficiaryService = beneficiaryService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponseDto> create(@RequestBody BeneficiaryRequestDto request,
                                                          Authentication authentication) {
        long userId = resolveUserId(authentication);
        BeneficiaryResponseDto created = beneficiaryService.createBeneficiary(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns the beneficiary profile belonging to the currently logged-in user.
     */
    @GetMapping("/me")
    public ResponseEntity<BeneficiaryResponseDto> getMyProfile(Authentication authentication) {
        long userId = resolveUserId(authentication);
        return ResponseEntity.ok(beneficiaryService.getMyProfile(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeneficiaryResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaryById(id));
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponseDto>> getAll() {
        return ResponseEntity.ok(beneficiaryService.getAllBeneficiaries());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BeneficiaryResponseDto> update(@PathVariable Long id,
                                                          @RequestBody BeneficiaryRequestDto request,
                                                          Authentication authentication) {
        long userId = resolveUserId(authentication);
        return ResponseEntity.ok(beneficiaryService.updateBeneficiary(id, request, userId));
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