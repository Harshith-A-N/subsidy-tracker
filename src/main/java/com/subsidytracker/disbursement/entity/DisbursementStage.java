package com.subsidytracker.disbursement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.subsidytracker.common.enums.TriggerMilestone;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "disbursement_stages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DisbursementStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private DisbursementPlan plan;

    @NotBlank
    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @NotNull
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @NotNull
    @Column(name = "percentage_of_grant", nullable = false)
    private BigDecimal percentageOfGrant;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_milestone", nullable = false)
    private TriggerMilestone triggerMilestone;
}
