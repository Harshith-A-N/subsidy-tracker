package com.subsidytracker.eligibility.dto;

import com.subsidytracker.common.enums.VerificationDecision;
import lombok.Data;

@Data
public class VerificationRequestDTO {
    private Long officerId;
    private VerificationDecision decision;
    private String remarks;
}
