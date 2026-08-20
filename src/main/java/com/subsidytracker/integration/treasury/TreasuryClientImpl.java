package com.subsidytracker.integration.treasury;

import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementRequest;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TreasuryClientImpl implements TreasuryClient {

    private final RestClient restClient;

    public TreasuryClientImpl(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://localhost:8080/mock/external-treasury")
                .build();
    }

    @Override
    public TreasuryDisbursementResponse processDisbursement(TreasuryDisbursementRequest request) {
        return restClient.post()
                .uri("/disburse")
                .body(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (req, res) -> {
                            throw new RuntimeException(
                                    "External treasury system returned HTTP " + res.getStatusCode());
                        })
                .body(TreasuryDisbursementResponse.class);
    }
}
