package com.subsidytracker.disbursement.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DisbursementPlanResponse {
    private Long id;
    private Long schemeId;
    private Integer numberOfStages;
    private Long createdById;
    private LocalDateTime createdAt;
    private List<DisbursementStageResponse> stages;
}
