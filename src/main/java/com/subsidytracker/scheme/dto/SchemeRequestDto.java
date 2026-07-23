package com.subsidytracker.scheme.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class SchemeRequestDto {
    private String name;
    private String description;
    private BigDecimal minIncome;
    private BigDecimal maxIncome;
    private String allowedCategories; // comma-separated, per your documented decision
    private boolean isActive;
    private String requiredDocuments;
}