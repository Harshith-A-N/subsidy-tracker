package com.subsidytracker.scheme.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("isActive")
    @JsonAlias({"active", "isActive"})
    private boolean isActive;

    private String requiredDocuments;

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isActive() {
        return this.isActive;
    }
}