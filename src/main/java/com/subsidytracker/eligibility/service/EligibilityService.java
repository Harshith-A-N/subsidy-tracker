package com.subsidytracker.eligibility.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.Document;
import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.eligibility.dto.ApplicationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EligibilityService {

    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public EligibilityService(ApplicationRepository applicationRepository,
                              DocumentRepository documentRepository,
                              UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ApplicationResponseDto calculateEligibility(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        Beneficiary beneficiary = application.getBeneficiary();
        Scheme scheme = application.getScheme();

        if (!scheme.isActive()) {
            throw new InvalidOperationException("Scheme '" + scheme.getName() + "' is not active.");
        }

        // ---- Step 1: mandatory objective checks (auto-reject path) ----
        if (!isIncomeValid(beneficiary, scheme)) {
            return finalize(application, ApplicationStatus.NOT_ELIGIBLE, 0,
                    "Beneficiary's annual income exceeds the scheme's maximum limit.");
        }

        if (!isCategoryValid(beneficiary, scheme)) {
            return finalize(application, ApplicationStatus.NOT_ELIGIBLE, 0,
                    "Beneficiary's category is not permitted for this scheme.");
        }

        // ---- Step 2: manual-review triggers (not a hard reject) ----
        if (!areDocumentsComplete(application, scheme)) {
            return finalize(application, ApplicationStatus.MANUAL_REVIEW_REQUIRED, 0,
                    "One or more required documents are missing.");
        }

        if (!hasFieldOfficerForRegion(beneficiary)) {
            return finalize(application, ApplicationStatus.MANUAL_REVIEW_REQUIRED, 0,
                    "No field officer is assigned to beneficiary's region: " + beneficiary.getRegion());
        }

        // ---- Step 3: passed all checks - calculate score ----
        double score = calculateScore(beneficiary, scheme);

        // Score threshold decisions could vary, but per your docs, passing all mandatory
        // checks + having docs + having a routable officer means it's ready to enter
        // the verification workflow.
        return finalize(application, ApplicationStatus.FIELD_VERIFICATION_PENDING, score,
                "Eligibility passed. Score: " + score + ". Routed to field officer for region: "
                        + beneficiary.getRegion());
    }

    // ======================== CHECKS ========================

    private boolean isIncomeValid(Beneficiary beneficiary, Scheme scheme) {
        if (scheme.getMaxIncome() == null) return true;
        if (beneficiary.getAnnualIncome() == null) return false;
        return beneficiary.getAnnualIncome().compareTo(scheme.getMaxIncome()) <= 0;
    }

    private boolean isCategoryValid(Beneficiary beneficiary, Scheme scheme) {
        if (scheme.getAllowedCategories() == null || scheme.getAllowedCategories().isBlank()) return true;
        if (beneficiary.getCategory() == null) return false;

        String category = beneficiary.getCategory().name();
        return Arrays.stream(scheme.getAllowedCategories().split(","))
                .map(String::trim)
                .anyMatch(c -> c.equalsIgnoreCase(category));
    }

    private boolean areDocumentsComplete(Application application, Scheme scheme) {
        if (scheme.getRequiredDocuments() == null || scheme.getRequiredDocuments().isBlank()) return true;

        List<Document> uploaded = documentRepository.findByApplicationId(application.getId());
        Set<String> uploadedTypes = uploaded.stream()
                .map(d -> d.getDocumentType().trim().toLowerCase())
                .collect(Collectors.toSet());

        List<String> required = Arrays.stream(scheme.getRequiredDocuments().split(","))
                .map(String::trim).map(String::toLowerCase)
                .collect(Collectors.toList());

        return uploadedTypes.containsAll(required);
    }

    private boolean hasFieldOfficerForRegion(Beneficiary beneficiary) {
        if (beneficiary.getRegion() == null || beneficiary.getRegion().isBlank()) return false;
        return !userRepository.findByRoleAndRegion(Role.FIELD_OFFICER, beneficiary.getRegion()).isEmpty();
    }

    // ======================== SCORE (only reached if all checks pass) ========================

    private double calculateScore(Beneficiary beneficiary, Scheme scheme) {
        double score = 0;

        // Lower income relative to scheme's max = higher priority score (0-50)
        if (scheme.getMaxIncome() != null && beneficiary.getAnnualIncome() != null
                && scheme.getMaxIncome().signum() > 0) {
            double ratio = beneficiary.getAnnualIncome().doubleValue() / scheme.getMaxIncome().doubleValue();
            ratio = Math.min(Math.max(ratio, 0), 1); // clamp between 0 and 1
            score += (1 - ratio) * 50;
        } else {
            score += 50;
        }

        // Profile completeness (0-50): reward having a full, usable profile
        if (beneficiary.getAddress() != null && !beneficiary.getAddress().isBlank()) score += 15;
        if (beneficiary.getPhoneNumber() != null && !beneficiary.getPhoneNumber().isBlank()) score += 15;
        if (beneficiary.getRegion() != null && !beneficiary.getRegion().isBlank()) score += 20;

        return Math.round(score * 100.0) / 100.0;
    }

    // ======================== FINALIZE ========================

    private ApplicationResponseDto finalize(Application application, ApplicationStatus status,
                                            double score, String remarks) {
        application.setStatus(status);
        application.setEligibilityScore(score);
        application.setRemarks(remarks);  // <-- new line
        Application saved = applicationRepository.save(application);

        ApplicationResponseDto dto = new ApplicationResponseDto();
        dto.setId(saved.getId());
        dto.setBeneficiaryId(saved.getBeneficiary().getId());
        dto.setBeneficiaryName(saved.getBeneficiary().getFullName());
        dto.setSchemeId(saved.getScheme().getId());
        dto.setSchemeName(saved.getScheme().getName());
        dto.setStatus(saved.getStatus());
        dto.setEligibilityScore(saved.getEligibilityScore());
        dto.setSubmissionDate(saved.getSubmissionDate());
        dto.setRemarks(saved.getRemarks());  // <-- new line
        return dto;
    }
}