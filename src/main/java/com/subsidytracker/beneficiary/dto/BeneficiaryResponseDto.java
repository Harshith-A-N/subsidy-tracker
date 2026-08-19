package com.subsidytracker.beneficiary.dto;

import com.subsidytracker.common.enums.BeneficiaryCategory;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class BeneficiaryResponseDto {
    private Long id;
    private String fullName;
    private String nationalIdNumber;
    private String phoneNumber;
    private String address;
    private BeneficiaryCategory category;
    private LocalDate registrationDate;
    private String region;
    private BigDecimal annualIncome;
    private Long userId;
    private String email;
}