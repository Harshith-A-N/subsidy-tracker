package com.subsidytracker.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name="schemes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Scheme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    // Eligibility criteria - structured fields for Module 2's scoring engine
    private BigDecimal minIncome;

    private BigDecimal maxIncome;

    // e.g. "SC,ST,OBC" — comma-separated list of allowed BeneficiaryCategory values
    // (kept simple for now; could normalize into its own table later if needed)
    private String allowedCategories;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(name = "required_documents", length = 1000)
    private String requiredDocuments; // comma-separated list, e.g. "AADHAR,INCOME_CERTIFICATE,ADDRESS_PROOF"

    // NOTE: not exposed via SchemeRequestDto/SchemeService — grant amounts are
    // driven entirely by SchemeSlab (per beneficiary category) everywhere in
    // the codebase. This column is dead; kept nullable rather than dropped to
    // avoid an unnecessary schema change. Do not wire this up — use SchemeSlab.
    @Column(name = "grant_amount", precision = 15, scale = 2)
    private BigDecimal grantAmount = BigDecimal.ZERO;

    // NOTE: also not exposed via any DTO. Was previously NOT NULL, which silently
    // broke scheme creation (every INSERT failed with a constraint violation,
    // since nothing ever set it). Scheme-level budget totals are now derived from
    // summing RegionalBudget rows in AnalyticsRepository.getSchemeUtilizations()
    // instead of relying on this column.
    @Column(name = "total_budget", precision = 15, scale = 2)
    private BigDecimal totalBudget = BigDecimal.ZERO;
}
