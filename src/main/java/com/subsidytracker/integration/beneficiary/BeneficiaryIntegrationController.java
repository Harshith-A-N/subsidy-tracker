package com.subsidytracker.integration.beneficiary;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subsidytracker.integration.beneficiary.dto.BeneficiaryValidationRequest;
import com.subsidytracker.integration.beneficiary.dto.BeneficiaryValidationResponse;

@RestController
@RequestMapping("/api/v1/integrations/beneficiary")
public class BeneficiaryIntegrationController {

    private final BeneficiaryRegistryClient beneficiaryRegistryClient;

    public BeneficiaryIntegrationController(
            BeneficiaryRegistryClient beneficiaryRegistryClient) {

        this.beneficiaryRegistryClient = beneficiaryRegistryClient;
    }

    @PostMapping("/validate")
    public ResponseEntity<BeneficiaryValidationResponse> validateBeneficiary(
            @RequestBody BeneficiaryValidationRequest request) {

        return ResponseEntity.ok(
                beneficiaryRegistryClient.validateBeneficiary(request)
        );
    }
}