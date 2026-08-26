package com.subsidytracker.eligibility.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.Document;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.common.enums.DisbursementScheduleStatus;
import com.subsidytracker.common.enums.DocumentVerificationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.common.service.CloudinaryService;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementMilestoneRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.eligibility.dto.DocumentResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import com.subsidytracker.eligibility.repository.UserRepository;

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
    private final AuditLogService auditLogService;

    public DocumentService(DocumentRepository documentRepository,
                           ApplicationRepository applicationRepository,
                           UserRepository userRepository,
                           CloudinaryService cloudinaryService,
                           DisbursementStageRepository stageRepository,
                           ApplicationDisbursementScheduleRepository scheduleRepository,
                           DisbursementMilestoneRepository milestoneRepository,
                           AuditLogService auditLogService) {
        this.documentRepository = documentRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.stageRepository = stageRepository;
        this.scheduleRepository = scheduleRepository;
        this.milestoneRepository = milestoneRepository;
        this.auditLogService = auditLogService;
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

    /**
     * Marks a document VERIFIED/REJECTED/PENDING.
     *
     * Previously took only (documentId, status, remarks) - no caller identity
     * at all. That meant:
     *   - documentId was never checked against the applicationId in the URL,
     *     so any authenticated officer could verify a document belonging to
     *     a completely different (possibly out-of-region) application just
     *     by knowing/guessing its id.
     *   - there was no role/region/stage check, so a District Officer or
     *     Finance Approver could set VERIFIED on a KYC document the Field
     *     Officer never actually looked at - which would have silently
     *     defeated the "all documents must be VERIFIED before Field
     *     approval" gate in VerificationService.
     *   - nothing was audit-logged, unlike every other state-changing
     *     action in this codebase.
     * This mirrors the same ownership/region/stage checks VerificationService
     * enforces for the Field-level approval itself, since this is the one
     * action that approval gate ultimately depends on.
     */
    @Transactional
    public DocumentResponseDto verifyDocument(Long applicationId, Long documentId,
                                              DocumentVerificationStatus status, String remarks,
                                              long currentUserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        // IDOR check: the document must actually belong to the application in the URL.
        if (!document.getApplication().getId().equals(applicationId)) {
            throw new ResourceNotFoundException("Document", documentId);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (currentUser.getRole() != Role.ADMIN) {
            // KYC documents (Aadhar, Land Record, Income Certificate, ...) are the
            // Field Officer's responsibility to check - see DocumentVerificationStatus.
            // Stage-linked utilization proofs are verified through the compliance
            // milestone workflow (ComplianceMilestoneService), not this endpoint.
            if (currentUser.getRole() != Role.FIELD_OFFICER) {
                throw new InvalidOperationException(
                        "Only the assigned Field Officer (or an Administrator) may verify KYC documents.");
            }

            if (document.getStage() != null) {
                throw new InvalidOperationException(
                        "This endpoint verifies KYC documents only. Utilization proofs are handled "
                                + "through the compliance milestone workflow.");
            }

            if (application.getStatus() != ApplicationStatus.FIELD_VERIFICATION_PENDING) {
                throw new InvalidOperationException(
                        "Documents can only be verified while the application is pending Field review. "
                                + "Current status: " + application.getStatus());
            }

            String beneficiaryRegion = application.getBeneficiary().getRegion();
            String officerRegion = currentUser.getRegion();
            if (beneficiaryRegion == null || officerRegion == null
                    || !beneficiaryRegion.equalsIgnoreCase(officerRegion)) {
                throw new InvalidOperationException("This application is not in your assigned region.");
            }
        }

        document.setVerificationStatus(status);
        document.setRemarks(remarks);

        Document saved = documentRepository.save(document);

        try {
            auditLogService.logEvent(
                    "Document",
                    saved.getId(),
                    "DOCUMENT_" + status.name(),
                    currentUser,
                    "Set verification status " + status + " for document type '" + saved.getDocumentType()
                            + "' on application id: " + applicationId);
        } catch (Exception e) {
            // Audit log failure must not prevent primary operation success
        }

        return toDto(saved);
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

        boolean stageOnly = restrictToStageDocuments(application, currentUserId);

        return documentRepository.findByApplicationId(applicationId).stream()
                .filter(d -> !stageOnly || d.getStage() != null)
                .map(this::toDto)
                .toList();
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

        // During the disbursement lifecycle, officers may only open stage-linked
        // proofs — never KYC documents (stage == null). See restrictToStageDocuments.
        if (restrictToStageDocuments(application, currentUserId) && document.getStage() == null) {
            throw new InvalidOperationException("You are not authorized to view this document.");
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
                // Verification stage (KYC) OR disbursement lifecycle (stage proofs only —
                // enforced by restrictToStageDocuments in the calling methods).
                validateOfficerDocumentAccess(application, currentUser,
                        Set.of(ApplicationStatus.FIELD_VERIFICATION_PENDING,
                                ApplicationStatus.READY_FOR_DISBURSEMENT,
                                ApplicationStatus.DISBURSED));
                break;
            case DISTRICT_OFFICER:
                validateOfficerDocumentAccess(application, currentUser,
                        Set.of(ApplicationStatus.DISTRICT_REVIEW_PENDING,
                                ApplicationStatus.READY_FOR_DISBURSEMENT,
                                ApplicationStatus.DISBURSED));
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
     * During the disbursement lifecycle (READY_FOR_DISBURSEMENT / DISBURSED),
     * field and district officers are permitted (by checkDocumentAccess) to reach
     * an application's documents so they can verify utilization proofs — but they
     * must NOT see KYC documents (stage == null) at that point. This returns true
     * when the caller is an officer viewing an application in the disbursement
     * phase, signalling the read methods to expose stage-linked documents only.
     * KYC access during the officer's own verification stage is unaffected.
     */
    private boolean restrictToStageDocuments(Application application, long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
        Role role = currentUser.getRole();
        if (role != Role.FIELD_OFFICER && role != Role.DISTRICT_OFFICER) {
            return false;
        }
        ApplicationStatus status = application.getStatus();
        return status == ApplicationStatus.READY_FOR_DISBURSEMENT
                || status == ApplicationStatus.DISBURSED;
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