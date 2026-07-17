package com.subsidytracker.common.entity;

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

    // Comma-separated list of mandatory documents required for this scheme
    @Column(name = "required_documents", length = 1000)
    private String requiredDocuments;

    @Column(nullable = false)
    private boolean isActive = true;
}
