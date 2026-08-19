package com.subsidytracker.beneficiary.dto;

import com.subsidytracker.common.enums.BeneficiaryCategory;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class BeneficiaryRequestDto {
    private String fullName;
    private String nationalIdNumber;
    private String phoneNumber;
    private String address;
    private BeneficiaryCategory category;
    private String region;
    private BigDecimal annualIncome;
}