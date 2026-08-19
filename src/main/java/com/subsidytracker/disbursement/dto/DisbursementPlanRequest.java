package com.subsidytracker.disbursement.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DisbursementPlanRequest {
    private Long schemeId;
    private List<DisbursementStageRequest> stages;
}
