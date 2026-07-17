package com.subsidytracker.eligibility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResponseDTO {
    private Long verificationId;
    private Long applicationId;
    private String newStatus;
    private String newStage;
    private String message;
}
