package com.subsidytracker.disbursement.dto;

import com.subsidytracker.common.enums.DisbursementScheduleStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ScheduleEntryResponse {
    private Long id;
    private Long applicationId;
    private Long stageId;
    private String stageName;
    private Integer stageSequenceNumber;
    private BigDecimal scheduledAmount;
    private LocalDate dueDate;
    private DisbursementScheduleStatus status;
}
