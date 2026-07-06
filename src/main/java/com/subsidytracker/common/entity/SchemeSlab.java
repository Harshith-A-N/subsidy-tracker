package com.subsidytracker.common.entity;

import com.subsidytracker.common.enums.BeneficiaryCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "scheme_slabs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemeSlab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    private Scheme scheme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeneficiaryCategory category;

    @Column(nullable = false)
    private BigDecimal grantAmount;
}
