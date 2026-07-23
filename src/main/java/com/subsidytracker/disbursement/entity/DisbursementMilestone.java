package com.subsidytracker.disbursement.entity;

import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.common.enums.DisbursementStatus;
import com.subsidytracker.common.enums.MilestoneType;
import com.subsidytracker.common.entity.Application;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class DisbursementMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Application application;

    @Enumerated(EnumType.STRING)
    private MilestoneType milestoneType;

    private int sequenceOrder;

    private BigDecimal scheduledAmount;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus;

    @Enumerated(EnumType.STRING)
    private DisbursementStatus disbursementStatus;

    private LocalDate actualDisbursedDate;
}