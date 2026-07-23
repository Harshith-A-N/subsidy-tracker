package com.subsidytracker.scheme.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class RegionalBudgetDto {
    private Long id;
    private Long schemeId;
    private String regionName;
    private BigDecimal allocatedBudget;
    private BigDecimal utilizedBudget;
}