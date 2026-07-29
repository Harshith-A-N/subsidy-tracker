package com.subsidytracker.eligibility.controller;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.DocumentVerificationStatus;
import com.subsidytracker.eligibility.dto.DocumentResponseDto;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService,
                              UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponseDto> upload(@PathVariable Long applicationId,
                                                      @RequestParam String documentType,
                                                      @RequestParam("file") MultipartFile file,
                                                      Authentication authentication) {
        long userId = resolveUserId(authentication);
        DocumentResponseDto saved = documentService.uploadDocument(applicationId, documentType, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponseDto>> getAll(@PathVariable Long applicationId,
                                                            Authentication authentication) {
        long userId = resolveUserId(authentication);
        return ResponseEntity.ok(documentService.getDocumentsForApplication(applicationId, userId));
    }

    @PatchMapping("/{documentId}/verify")
    public ResponseEntity<DocumentResponseDto> verify(@PathVariable Long applicationId,
                                                      @PathVariable Long documentId,
                                                      @RequestParam DocumentVerificationStatus status,
                                                      @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(documentService.verifyDocument(documentId, status, remarks));
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