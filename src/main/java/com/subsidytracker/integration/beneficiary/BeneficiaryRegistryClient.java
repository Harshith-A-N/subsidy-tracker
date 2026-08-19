package com.subsidytracker.integration.beneficiary;

import com.subsidytracker.integration.beneficiary.dto.BeneficiaryValidationRequest;
import com.subsidytracker.integration.beneficiary.dto.BeneficiaryValidationResponse;

public interface BeneficiaryRegistryClient {

    BeneficiaryValidationResponse validateBeneficiary(
            BeneficiaryValidationRequest request);
}