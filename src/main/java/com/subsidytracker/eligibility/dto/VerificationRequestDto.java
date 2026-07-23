package com.subsidytracker.eligibility.dto;

import com.subsidytracker.common.enums.VerificationDecision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationRequestDto {
    private Long officerId;
    private VerificationDecision decision;
    private String remarks;
}