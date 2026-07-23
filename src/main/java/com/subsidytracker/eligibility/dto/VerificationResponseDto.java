package com.subsidytracker.eligibility.dto;

import com.subsidytracker.common.enums.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationResponseDto {
    private Long verificationId;
    private Long applicationId;
    private ApplicationStatus newStatus;
    private String message;
}