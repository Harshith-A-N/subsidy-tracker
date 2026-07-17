package com.subsidytracker.eligibility.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.eligibility.dto.EligibilityScoreDTO;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core eligibility scoring engine for Module 2.
 *
 * Implements strict scoring logic:
 * Step 1: Mandatory Eligibility Checks (Income, Category, Documents)
 * Step 2: Scoring out of 100
 *         - Profile Completeness (20 points)
 *         - Document Completeness (30 points)
 *         - Income Priority (30 points)
 *         - Address / Location Completeness (20 points)
 * Step 3: Final Decision (Threshold = 40)
 */
@Service
public class EligibilityService {

    private final ApplicationRepository applicationRepository;

    // Constructor injection
    public EligibilityService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    /**
     * Calculates the eligibility score and updates application status.
     *
     * @param applicationId the ID of the application to score
     * @return EligibilityScoreDTO with score, status, and remarks
     */
    @Transactional
    public EligibilityScoreDTO calculateEligibility(Long applicationId) {

        // 1. Read the Application
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        // 2. Fetch Beneficiary
        Beneficiary beneficiary = application.getBeneficiary();

        // 3. Fetch Scheme
        Scheme scheme = application.getScheme();

        if (!scheme.isActive()) {
            throw new InvalidOperationException("Scheme '" + scheme.getName() + "' is no longer active.");
        }

        StringBuilder remarks = new StringBuilder();

        // 4. Validate mandatory eligibility conditions
        boolean isIncomeValid = validateIncome(beneficiary, scheme);
        boolean isCategoryValid = validateCategory(beneficiary, scheme);
        boolean isDocumentsValid = validateMandatoryDocuments(application, scheme);

        // 5. If any mandatory condition fails
        if (!isIncomeValid || !isCategoryValid || !isDocumentsValid) {
            application.setStatus(ApplicationStatus.NOT_ELIGIBLE);
            application.setEligibilityScore(0);
            
            if (!isIncomeValid) remarks.append("Income exceeds scheme limits. ");
            if (!isCategoryValid) remarks.append("Category is not allowed for this scheme. ");
            if (!isDocumentsValid) remarks.append("Missing required documents. ");
            
            application.setRemarks(remarks.toString().trim());
            applicationRepository.save(application);

            return EligibilityScoreDTO.builder()
                    .eligibilityScore(0)
                    .eligibilityStatus(ApplicationStatus.NOT_ELIGIBLE.name())
                    .remarks(application.getRemarks())
                    .build();
        }

        // 6. Otherwise calculate an eligibility score out of 100
        double profileScore = calculateProfileScore(beneficiary);
        double documentScore = calculateDocumentScore(application, scheme);
        double incomeScore = calculateIncomePriorityScore(beneficiary, scheme);
        double locationScore = calculateLocationScore(beneficiary);

        double totalScore = profileScore + documentScore + incomeScore + locationScore;
        // Round to 2 decimal places for cleaner output
        totalScore = Math.round(totalScore * 100.0) / 100.0;

        // 7. & 8. Set status based on score (Threshold 40)
        if (totalScore >= 40) {
            application.setStatus(ApplicationStatus.ELIGIBLE);
            remarks.append("Application is eligible. Score breakdown: Profile=").append(profileScore)
                   .append(", Docs=").append(documentScore)
                   .append(", Income=").append(incomeScore)
                   .append(", Location=").append(locationScore).append(".");
        } else {
            application.setStatus(ApplicationStatus.NOT_ELIGIBLE);
            remarks.append("Score below eligibility threshold (40). Score breakdown: Profile=").append(profileScore)
                   .append(", Docs=").append(documentScore)
                   .append(", Income=").append(incomeScore)
                   .append(", Location=").append(locationScore).append(".");
        }
        
        application.setRemarks(remarks.toString());

        // 9. Save the eligibility score inside the Application entity
        application.setEligibilityScore(totalScore);
        applicationRepository.save(application);

        // 10. Return a DTO
        return EligibilityScoreDTO.builder()
                .eligibilityScore(totalScore)
                .eligibilityStatus(application.getStatus().name())
                .remarks(application.getRemarks())
                .build();
    }

    // ======================== MANDATORY CONDITIONS (Step 1) ========================

    private boolean validateIncome(Beneficiary beneficiary, Scheme scheme) {
        if (scheme.getMaxIncome() == null) return true;
        if (beneficiary.getAnnualIncome() == null) return false;
        return beneficiary.getAnnualIncome().compareTo(scheme.getMaxIncome()) <= 0;
    }

    private boolean validateCategory(Beneficiary beneficiary, Scheme scheme) {
        if (scheme.getAllowedCategories() == null || scheme.getAllowedCategories().isBlank()) return true;
        if (beneficiary.getCategory() == null) return false;
        
        String beneficiaryCategory = beneficiary.getCategory().name();
        return Arrays.stream(scheme.getAllowedCategories().split(","))
                .map(String::trim)
                .anyMatch(cat -> cat.equalsIgnoreCase(beneficiaryCategory));
    }

    private boolean validateMandatoryDocuments(Application application, Scheme scheme) {
        if (scheme.getRequiredDocuments() == null || scheme.getRequiredDocuments().isBlank()) return true;
        if (application.getUploadedDocuments() == null || application.getUploadedDocuments().isBlank()) return false;
        
        Set<String> uploadedDocs = Arrays.stream(application.getUploadedDocuments().split(","))
                .map(String::trim).map(String::toLowerCase).collect(Collectors.toSet());
                
        List<String> requiredDocs = Arrays.stream(scheme.getRequiredDocuments().split(","))
                .map(String::trim).map(String::toLowerCase).collect(Collectors.toList());
                
        return uploadedDocs.containsAll(requiredDocs);
    }

    // ======================== SCORING RULES (Step 2) ========================

    /**
     * Profile Completeness Check (20 points max).
     * Distributes 5 points each for Address, District, State, and Phone.
     */
    private double calculateProfileScore(Beneficiary beneficiary) {
        double score = 0;
        if (beneficiary.getAddress() != null && !beneficiary.getAddress().isBlank()) score += 5.0;
        if (beneficiary.getDistrict() != null && !beneficiary.getDistrict().isBlank()) score += 5.0;
        if (beneficiary.getState() != null && !beneficiary.getState().isBlank()) score += 5.0;
        if (beneficiary.getPhoneNumber() != null && !beneficiary.getPhoneNumber().isBlank()) score += 5.0;
        return score;
    }

    /**
     * Document Completeness (30 points max).
     * Since all required documents must be uploaded to pass mandatory checks, this will give 30 points if required docs exist.
     * Calculated proportionally if we ever allow partial document validation in the future.
     */
    private double calculateDocumentScore(Application application, Scheme scheme) {
        if (scheme.getRequiredDocuments() == null || scheme.getRequiredDocuments().isBlank()) {
            return 30.0; // If no documents are required, grant full points
        }
        
        long reqCount = Arrays.stream(scheme.getRequiredDocuments().split(",")).count();
        if (reqCount == 0) return 30.0;
        
        Set<String> uploadedDocs = (application.getUploadedDocuments() == null) ? Set.of() :
                Arrays.stream(application.getUploadedDocuments().split(","))
                .map(String::trim).map(String::toLowerCase).collect(Collectors.toSet());
                
        long uploadedReqCount = Arrays.stream(scheme.getRequiredDocuments().split(","))
                .map(String::trim).map(String::toLowerCase)
                .filter(uploadedDocs::contains)
                .count();
                
        return ((double) uploadedReqCount / reqCount) * 30.0;
    }

    /**
     * Income Priority (30 points max).
     * Calculated proportionally based on the beneficiary's income relative to the scheme's max income.
     * The lower the income, the higher the score.
     */
    private double calculateIncomePriorityScore(Beneficiary beneficiary, Scheme scheme) {
        if (scheme.getMaxIncome() == null || scheme.getMaxIncome().compareTo(BigDecimal.ZERO) == 0) {
            return 30.0; // Automatically get full points if there's no income limit or limit is zero
        }
        
        BigDecimal maxIncome = scheme.getMaxIncome();
        BigDecimal benIncome = beneficiary.getAnnualIncome();
        
        if (benIncome == null) benIncome = BigDecimal.ZERO;
        if (benIncome.compareTo(maxIncome) >= 0) return 0.0;
        if (benIncome.compareTo(BigDecimal.ZERO) <= 0) return 30.0;
        
        // score = (1.0 - (benIncome / maxIncome)) * 30
        BigDecimal ratio = benIncome.divide(maxIncome, 4, RoundingMode.HALF_UP);
        double ratioDouble = ratio.doubleValue();
        return (1.0 - ratioDouble) * 30.0;
    }

    /**
     * Address / Location Completeness (20 points max).
     * Distributes appropriately: Address (6.66), District (6.66), State (6.66).
     */
    private double calculateLocationScore(Beneficiary beneficiary) {
        double score = 0;
        if (beneficiary.getAddress() != null && !beneficiary.getAddress().isBlank()) score += 6.66;
        if (beneficiary.getDistrict() != null && !beneficiary.getDistrict().isBlank()) score += 6.66;
        if (beneficiary.getState() != null && !beneficiary.getState().isBlank()) score += 6.66;
        return score;
    }
}
