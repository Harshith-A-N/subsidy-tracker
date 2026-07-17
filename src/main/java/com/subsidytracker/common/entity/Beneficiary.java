package com.subsidytracker.common.entity;

import com.subsidytracker.common.enums.BeneficiaryCategory;
import  jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="beneficiaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String nationalIdNumber;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeneficiaryCategory category;

    @Column(nullable = false)
    private LocalDate registrationDate;

    // --- Module 2 additions (needed for eligibility scoring) ---

    // Annual income used to check against scheme income limits
    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    // District where the beneficiary resides
    @Column(length = 100)
    private String district;

    // State where the beneficiary resides
    @Column(length = 100)
    private String state;
}
