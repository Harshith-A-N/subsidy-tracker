package com.subsidytracker.integration.beneficiary;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.subsidytracker.integration.beneficiary.dto.BeneficiaryValidationRequest;
import com.subsidytracker.integration.beneficiary.dto.BeneficiaryValidationResponse;

@Service
public class BeneficiaryRegistryClientImpl
        implements BeneficiaryRegistryClient {

    private final RestClient restClient;

    public BeneficiaryRegistryClientImpl(RestClient.Builder restClientBuilder) {

        this.restClient = restClientBuilder
                .baseUrl("http://localhost:8080/mock/external-registry")
                .build();
    }

    @Override
    public BeneficiaryValidationResponse validateBeneficiary(
            BeneficiaryValidationRequest request) {

        return restClient.post()
                .uri("/validate")
                .body(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (req, res) -> {
                            throw new RuntimeException(
                                    "External beneficiary registry returned HTTP "
                                            + res.getStatusCode());
                        })
                .body(BeneficiaryValidationResponse.class);
    }
}