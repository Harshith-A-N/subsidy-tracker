package com.subsidytracker.scheme.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SchemeResponseDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal minIncome;
    private BigDecimal maxIncome;
    private String allowedCategories;
    
    @JsonProperty("isActive")
    private boolean isActive;

    @JsonProperty("active")
    public boolean isActiveStatus() {
        return isActive;
    }

    private String requiredDocuments;
    private List<SchemeSlabDto> slabs;
}