package com.subsidytracker.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "regional_budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionalBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    private Scheme scheme;

    @Column(nullable = false)
    private String regionName;

    @Column(nullable = false)
    private BigDecimal allocatedBudget;

    @Column(nullable = false)
    private BigDecimal utilizedBudget = new BigDecimal("0.00");
}