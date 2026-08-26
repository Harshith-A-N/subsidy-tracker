package com.subsidytracker.eligibility.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.subsidytracker.common.entity.Document;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.DocumentVerificationStatus;
import com.subsidytracker.eligibility.dto.DocumentResponseDto;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.service.DocumentService;

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
                                                      @RequestParam(required = false) Long stageId,
                                                      Authentication authentication) {
        long userId = resolveUserId(authentication);
        DocumentResponseDto saved = documentService.uploadDocument(applicationId, documentType, file, userId, stageId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponseDto>> getAll(@PathVariable Long applicationId,
                                                            Authentication authentication) {
        long userId = resolveUserId(authentication);
        return ResponseEntity.ok(documentService.getDocumentsForApplication(applicationId, userId));
    }

    /**
     * Streams the actual uploaded file for viewing/downloading — e.g. so a
     * Field Officer can open the Aadhaar scan they're verifying, not just
     * see its metadata. Reuses the same role/region access rules as
     * getAll() (see DocumentService.checkDocumentAccess).
     */
    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> getFile(@PathVariable Long applicationId,
                                            @PathVariable Long documentId,
                                            Authentication authentication) throws IOException {
        long userId = resolveUserId(authentication);
        Document document = documentService.getDocumentFileForDownload(applicationId, documentId, userId);

        String pathOrUrl = document.getFilePath();
        if (pathOrUrl != null && (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://"))) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, pathOrUrl)
                    .build();
        }

        Path filePath = Paths.get(pathOrUrl).normalize();
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        // Original filename was stored as "<uuid>_<originalName>" — strip the uuid prefix back off for display
        String storedName = filePath.getFileName().toString();
        String displayName = storedName.contains("_") ? storedName.substring(storedName.indexOf('_') + 1) : storedName;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(displayName).build().toString())
                .body(resource);
    }

    @PatchMapping("/{documentId}/verify")
    public ResponseEntity<DocumentResponseDto> verify(@PathVariable Long applicationId,
                                                      @PathVariable Long documentId,
                                                      @RequestParam DocumentVerificationStatus status,
                                                      @RequestParam(required = false) String remarks,
                                                      Authentication authentication) {
        long userId = resolveUserId(authentication);
        return ResponseEntity.ok(documentService.verifyDocument(applicationId, documentId, status, remarks, userId));
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