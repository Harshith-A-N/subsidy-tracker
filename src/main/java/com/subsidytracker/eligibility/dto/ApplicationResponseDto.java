package com.subsidytracker.eligibility.dto;

import com.subsidytracker.common.enums.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;
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
}