package com.subsidytracker.integration.beneficiary.dto;

public class BeneficiaryValidationResponse {

    private String beneficiaryId;
    private boolean valid;
    private String message;

    public BeneficiaryValidationResponse() {
    }

    public BeneficiaryValidationResponse(
            String beneficiaryId,
            boolean valid,
            String message) {

        this.beneficiaryId = beneficiaryId;
        this.valid = valid;
        this.message = message;
    }

    public String getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(String beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}