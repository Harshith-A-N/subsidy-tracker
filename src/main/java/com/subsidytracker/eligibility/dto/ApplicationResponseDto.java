package com.subsidytracker.eligibility.dto;

import com.subsidytracker.common.enums.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ApplicationResponseDto {
    private Long id;
    private Long beneficiaryId;
    private String beneficiaryName;
    private Long schemeId;
    private String schemeName;
    private ApplicationStatus status;
    private double eligibilityScore;
    private LocalDate submissionDate;
    private String remarks;
    private BigDecimal requestedAmount;
    private java.time.LocalDateTime verificationDate;
    private BeneficiarySummaryDto beneficiary;
    private SchemeSummaryDto scheme;

    @Getter
    @Setter
    public static class BeneficiarySummaryDto {
        private Long id;
        private String fullName;
        private String nationalIdNumber;
        private String phoneNumber;
        private String region;
        private String category;
    }

    @Getter
    @Setter
    public static class SchemeSummaryDto {
        private Long id;
        private String schemeName;
        private String name;
        private String description;
        private BigDecimal maxGrantAmount;
        private String requiredDocuments;
    }
}