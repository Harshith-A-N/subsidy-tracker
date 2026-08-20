package com.subsidytracker.eligibility.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.Document;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.DocumentVerificationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.common.service.CloudinaryService;
import com.subsidytracker.eligibility.dto.DocumentResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.common.enums.DisbursementScheduleStatus;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementMilestoneRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;

@Service
public class DocumentService {

    // Statuses that permit document uploads from the owning beneficiary
    private static final Set<ApplicationStatus> UPLOAD_ALLOWED_STATUSES = Set.of(
            ApplicationStatus.DRAFT,
            ApplicationStatus.RE_VERIFICATION_REQUIRED
    );

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final DisbursementStageRepository stageRepository;
    private final ApplicationDisbursementScheduleRepository scheduleRepository;
    private final DisbursementMilestoneRepository milestoneRepository;

    public DocumentService(DocumentRepository documentRepository,
                           ApplicationRepository applicationRepository,
                           UserRepository userRepository,
                           CloudinaryService cloudinaryService,
                           DisbursementStageRepository stageRepository,
                           ApplicationDisbursementScheduleRepository scheduleRepository,
                           DisbursementMilestoneRepository milestoneRepository) {
        this.documentRepository = documentRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.stageRepository = stageRepository;
        this.scheduleRepository = scheduleRepository;
        this.milestoneRepository = milestoneRepository;
    }

    @Transactional
    public DocumentResponseDto uploadDocument(Long applicationId, String documentType,
                                              MultipartFile file, long currentUserId) {
        return uploadDocument(applicationId, documentType, file, currentUserId, null);
    }

    @Transactional
    public DocumentResponseDto uploadDocument(Long applicationId, String documentType,
                                              MultipartFile file, long currentUserId, Long stageId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        // Ownership: application must belong to the authenticated beneficiary
        validateBeneficiaryOwnership(application, currentUserId);

        if (file.isEmpty()) {
            throw new InvalidOperationException("Uploaded file is empty.");
        }

        DisbursementStage stage = null;
        if (stageId != null) {
            stage = stageRepository.findById(stageId)
                    .orElseThrow(() -> new ResourceNotFoundException("DisbursementStage", stageId));

            if (!stage.getPlan().getScheme().getId().equals(application.getScheme().getId())) {
                throw new InvalidOperationException("Referenced stage does not belong to this application's scheme.");
            }

            ApplicationDisbursementSchedule schedule = scheduleRepository.findByApplicationId(applicationId).stream()
                    .filter(s -> s.getStage().getId().equals(stageId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidOperationException("No schedule found for this application and stage."));

            if (schedule.getStatus() != DisbursementScheduleStatus.RELEASED) {
                throw new InvalidOperationException("Utilization proof can only be uploaded after the corresponding stage has been released.");
            }

            DisbursementMilestone milestone = milestoneRepository.findByApplicationIdOrderBySequenceOrderAsc(applicationId).stream()
                    .filter(m -> m.getStage().getId().equals(stageId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidOperationException("No milestone found for this stage."));

            if (milestone.getComplianceStatus() == ComplianceStatus.COMPLETED) {
                throw new InvalidOperationException("Compliance for this stage has already been completed.");
            }

            milestone.setComplianceStatus(ComplianceStatus.PROOF_SUBMITTED);
            milestoneRepository.save(milestone);
        } else {
            // Status gate for KYC documents: only DRAFT and RE_VERIFICATION_REQUIRED allow KYC uploads
            if (!UPLOAD_ALLOWED_STATUSES.contains(application.getStatus())) {
                throw new InvalidOperationException(
                        "Documents can only be uploaded when the application is in DRAFT or RE_VERIFICATION_REQUIRED status. "
                                + "Current status: " + application.getStatus());
            }
        }

        String cloudinaryUrl = cloudinaryService.upload(file);

        Document document = new Document();
        document.setApplication(application);
        document.setStage(stage);
        document.setDocumentType(documentType);
        document.setFilePath(cloudinaryUrl);
        document.setUploadedAt(LocalDateTime.now());
        document.setVerificationStatus(DocumentVerificationStatus.PENDING);

        Document saved = documentRepository.save(document);
        return toDto(saved);
    }

    @Transactional
    public DocumentResponseDto verifyDocument(Long documentId, DocumentVerificationStatus status, String remarks) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        document.setVerificationStatus(status);
        document.setRemarks(remarks);

        return toDto(documentRepository.save(document));
    }

    /**
     * Returns documents for an application with role-based access control.
     *
     * BENEFICIARY: can only view documents for their own applications.
     * FIELD_OFFICER, DISTRICT_OFFICER, FINANCE_APPROVER: can view documents
     *   for applications currently in their review stage.
     * ADMIN: full access.
     */
    public List<DocumentResponseDto> getDocumentsForApplication(Long applicationId, long currentUserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        checkDocumentAccess(application, currentUserId);

        return documentRepository.findByApplicationId(applicationId).stream().map(this::toDto).toList();
    }

    /**
     * Resolves the on-disk path for a document's file, after applying the
     * same role-based access rules as getDocumentsForApplication. Used by
     * DocumentController's file-download endpoint.
     */
    public Document getDocumentFileForDownload(Long applicationId, Long documentId, long currentUserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        checkDocumentAccess(application, currentUserId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (!document.getApplication().getId().equals(applicationId)) {
            throw new ResourceNotFoundException("Document", documentId);
        }

        return document;
    }

    /**
     * Role-based access control for viewing/downloading an application's documents.
     *
     * BENEFICIARY: can only view documents for their own applications.
     * FIELD_OFFICER, DISTRICT_OFFICER: can view documents for applications
     *   currently in their review stage, in their assigned region.
     * FINANCE_APPROVER: statewide jurisdiction, no region check.
     * ADMIN: full access.
     */
    private void checkDocumentAccess(Application application, long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        Role role = currentUser.getRole();

        switch (role) {
            case ADMIN:
                // Full access
                break;
            case BENEFICIARY:
                validateBeneficiaryOwnership(application, currentUserId);
                break;
            case FIELD_OFFICER:
                validateOfficerDocumentAccess(application, currentUser,
                        Set.of(ApplicationStatus.FIELD_VERIFICATION_PENDING));
                break;
            case DISTRICT_OFFICER:
                validateOfficerDocumentAccess(application, currentUser,
                        Set.of(ApplicationStatus.DISTRICT_REVIEW_PENDING));
                break;
            case FINANCE_APPROVER:
                if (application.getStatus() != ApplicationStatus.FINANCE_REVIEW_PENDING) {
                    throw new InvalidOperationException(
                            "Application is not in your review stage. Current status: " + application.getStatus());
                }
                break;
            default:
                throw new InvalidOperationException("Your role does not have access to application documents.");
        }
    }

    /**
     * Validates that the application belongs to the currently authenticated beneficiary.
     */
    private void validateBeneficiaryOwnership(Application application, long currentUserId) {
        Beneficiary beneficiary = application.getBeneficiary();
        if (beneficiary.getUser() == null || beneficiary.getUser().getId() != currentUserId) {
            throw new InvalidOperationException("You are not authorized to access this application's documents.");
        }
    }

    /**
     * Validates that a field or district officer can access documents for this application.
     * The application must be in the officer's review stage AND the officer's region
     * must match the beneficiary's region.
     */
    private void validateOfficerDocumentAccess(Application application, User officer,
                                               Set<ApplicationStatus> allowedStatuses) {
        if (!allowedStatuses.contains(application.getStatus())) {
            throw new InvalidOperationException(
                    "Application is not in your review stage. Current status: " + application.getStatus());
        }

        String beneficiaryRegion = application.getBeneficiary().getRegion();
        String officerRegion = officer.getRegion();
        if (beneficiaryRegion == null || officerRegion == null
                || !beneficiaryRegion.equalsIgnoreCase(officerRegion)) {
            throw new InvalidOperationException(
                    "This application is not in your assigned region.");
        }
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