package com.subsidytracker.integration.beneficiary;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subsidytracker.integration.beneficiary.dto.BeneficiaryValidationRequest;
import com.subsidytracker.integration.beneficiary.dto.BeneficiaryValidationResponse;

@RestController
@RequestMapping("/mock/external-registry")
public class BeneficiaryMockController {

    @PostMapping("/validate")
    public ResponseEntity<BeneficiaryValidationResponse> validate(
            @RequestBody BeneficiaryValidationRequest request) {

        if ("BEN001".equals(request.getBeneficiaryId())
                && "Ravi Kumar".equalsIgnoreCase(request.getFullName())) {

            return ResponseEntity.ok(
                    new BeneficiaryValidationResponse(
                            request.getBeneficiaryId(),
                            true,
                            "Beneficiary verified successfully"
                    )
            );
        }

        return ResponseEntity.ok(
                new BeneficiaryValidationResponse(
                        request.getBeneficiaryId(),
                        false,
                        "Beneficiary details could not be verified"
                )
        );
    }
}