package com.subsidytracker.eligibility.controller;

import com.subsidytracker.common.enums.DocumentVerificationStatus;
import com.subsidytracker.eligibility.dto.DocumentResponseDto;
import com.subsidytracker.eligibility.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponseDto> upload(@PathVariable Long applicationId,
                                                      @RequestParam String documentType,
                                                      @RequestParam("file") MultipartFile file) {
        DocumentResponseDto saved = documentService.uploadDocument(applicationId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponseDto>> getAll(@PathVariable Long applicationId) {
        return ResponseEntity.ok(documentService.getDocumentsForApplication(applicationId));
    }

    @PatchMapping("/{documentId}/verify")
    public ResponseEntity<DocumentResponseDto> verify(@PathVariable Long applicationId,
                                                      @PathVariable Long documentId,
                                                      @RequestParam DocumentVerificationStatus status,
                                                      @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(documentService.verifyDocument(documentId, status, remarks));
    }
}