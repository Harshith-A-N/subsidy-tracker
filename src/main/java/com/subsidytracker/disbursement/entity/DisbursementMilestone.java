package com.subsidytracker.disbursement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.common.enums.DisbursementStatus;
import com.subsidytracker.common.enums.MilestoneType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "disbursement_milestones")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DisbursementMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private DisbursementStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "milestone_type", nullable = false)
    private MilestoneType milestoneType;

    @Column(nullable = false)
    private int sequenceOrder;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private BigDecimal scheduledAmount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceStatus complianceStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisbursementStatus disbursementStatus;

    private LocalDate actualDisbursedDate;

    private LocalDateTime completedAt;
}