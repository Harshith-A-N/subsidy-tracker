package com.subsidytracker.integration.treasury;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementRequest;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TreasuryClientImplTest {

    private MockRestServiceServer mockServer;
    private TreasuryClientImpl treasuryClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        treasuryClient = new TreasuryClientImpl(builder, "http://localhost:8080/mock/external-treasury");
    }

    @Test
    void processDisbursement_ShouldSendPostRequestAndReturnResponse() throws Exception {
        TreasuryDisbursementRequest request = new TreasuryDisbursementRequest(
                100L, 10L, 50L, new BigDecimal("15000"), "ACC-12345", "Stage 1 Grant");

        TreasuryDisbursementResponse mockResponse = new TreasuryDisbursementResponse(
                "TXN-123", 100L, true, "Accepted by Treasury", "2026-08-19T22:00:00");

        mockServer.expect(requestTo("http://localhost:8080/mock/external-treasury/disburse"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        TreasuryDisbursementResponse response = treasuryClient.processDisbursement(request);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTransactionId()).isEqualTo("TXN-123");
        assertThat(response.getApplicationId()).isEqualTo(100L);
        mockServer.verify();
    }

    @Test
    void processDisbursement_WhenHttpError_ShouldThrowRuntimeException() {
        TreasuryDisbursementRequest request = new TreasuryDisbursementRequest(
                100L, 10L, 50L, new BigDecimal("15000"), "ACC-12345", "Stage 1 Grant");

        mockServer.expect(requestTo("http://localhost:8080/mock/external-treasury/disburse"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> treasuryClient.processDisbursement(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("External treasury system returned HTTP 500");

        mockServer.verify();
    }
}
