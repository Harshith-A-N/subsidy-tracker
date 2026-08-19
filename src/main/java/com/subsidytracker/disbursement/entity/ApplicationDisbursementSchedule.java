package com.subsidytracker.disbursement.entity;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.enums.DisbursementScheduleStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "application_disbursement_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDisbursementSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private DisbursementStage stage;

    @NotNull
    @Column(name = "scheduled_amount", nullable = false)
    private BigDecimal scheduledAmount;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DisbursementScheduleStatus status;
}
