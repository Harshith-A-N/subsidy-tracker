package com.subsidytracker.integration.treasury.dto;

import java.math.BigDecimal;

public class TreasuryDisbursementRequest {

    private Long applicationId;
    private Long scheduleId;
    private Long beneficiaryId;
    private BigDecimal amount;
    private String accountReference;
    private String remarks;

    public TreasuryDisbursementRequest() {
    }

    public TreasuryDisbursementRequest(
            Long applicationId,
            Long scheduleId,
            Long beneficiaryId,
            BigDecimal amount,
            String accountReference,
            String remarks) {

        this.applicationId = applicationId;
        this.scheduleId = scheduleId;
        this.beneficiaryId = beneficiaryId;
        this.amount = amount;
        this.accountReference = accountReference;
        this.remarks = remarks;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(Long beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAccountReference() {
        return accountReference;
    }

    public void setAccountReference(String accountReference) {
        this.accountReference = accountReference;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
