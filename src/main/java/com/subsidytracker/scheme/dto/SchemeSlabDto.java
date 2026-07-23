package com.subsidytracker.scheme.dto;

import com.subsidytracker.common.enums.BeneficiaryCategory;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class SchemeSlabDto {
    private Long id;
    private Long schemeId;
    private BeneficiaryCategory category;
    private BigDecimal grantAmount;
}