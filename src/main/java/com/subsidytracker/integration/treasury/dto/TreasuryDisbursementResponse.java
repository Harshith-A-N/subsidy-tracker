package com.subsidytracker.integration.treasury.dto;

public class TreasuryDisbursementResponse {

    private String transactionId;
    private Long applicationId;
    private boolean success;
    private String message;
    private String processedAt;

    public TreasuryDisbursementResponse() {
    }

    public TreasuryDisbursementResponse(
            String transactionId,
            Long applicationId,
            boolean success,
            String message,
            String processedAt) {

        this.transactionId = transactionId;
        this.applicationId = applicationId;
        this.success = success;
        this.message = message;
        this.processedAt = processedAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}
