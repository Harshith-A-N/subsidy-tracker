package com.subsidytracker.integration.beneficiary.dto;

public class BeneficiaryValidationRequest {

    private String beneficiaryId;
    private String fullName;

    public BeneficiaryValidationRequest() {
    }

    public BeneficiaryValidationRequest(String beneficiaryId, String fullName) {
        this.beneficiaryId = beneficiaryId;
        this.fullName = fullName;
    }

    public String getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(String beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}