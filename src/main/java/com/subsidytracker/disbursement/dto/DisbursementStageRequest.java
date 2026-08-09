package com.subsidytracker.disbursement.dto;

import com.subsidytracker.common.enums.TriggerMilestone;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DisbursementStageRequest {
    private String stageName;
    private Integer sequenceNumber;
    private BigDecimal percentageOfGrant;
    private TriggerMilestone triggerMilestone;
}
