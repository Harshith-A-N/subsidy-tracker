package com.subsidytracker.eligibility.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Document;
import com.subsidytracker.common.enums.DocumentVerificationStatus;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.eligibility.dto.DocumentResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    // Folder where uploaded files physically get saved (created if missing)
    private static final String UPLOAD_DIR = "uploads/documents";

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;

    public DocumentService(DocumentRepository documentRepository,
                           ApplicationRepository applicationRepository) {
        this.documentRepository = documentRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public DocumentResponseDto uploadDocument(Long applicationId, String documentType, MultipartFile file) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        if (file.isEmpty()) {
            throw new InvalidOperationException("Uploaded file is empty.");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Prefix with a random UUID so two different uploads never overwrite each other
            String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path targetPath = uploadPath.resolve(storedFileName);
            file.transferTo(targetPath);

            Document document = new Document();
            document.setApplication(application);
            document.setDocumentType(documentType);
            document.setFilePath(targetPath.toString());
            document.setUploadedAt(LocalDateTime.now());
            document.setVerificationStatus(DocumentVerificationStatus.PENDING);

            Document saved = documentRepository.save(document);
            return toDto(saved);

        } catch (IOException e) {
            throw new InvalidOperationException("Failed to store file: " + e.getMessage());
        }
    }

    @Transactional
    public DocumentResponseDto verifyDocument(Long documentId, DocumentVerificationStatus status, String remarks) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        document.setVerificationStatus(status);
        document.setRemarks(remarks);

        return toDto(documentRepository.save(document));
    }

    public List<DocumentResponseDto> getDocumentsForApplication(Long applicationId) {
        return documentRepository.findByApplicationId(applicationId).stream().map(this::toDto).toList();
    }

    private DocumentResponseDto toDto(Document d) {
        DocumentResponseDto dto = new DocumentResponseDto();
        dto.setId(d.getId());
        dto.setApplicationId(d.getApplication().getId());
        dto.setDocumentType(d.getDocumentType());
        dto.setFilePath(d.getFilePath());
        dto.setUploadedAt(d.getUploadedAt());
        dto.setVerificationStatus(d.getVerificationStatus());
        dto.setRemarks(d.getRemarks());
        return dto;
    }
}