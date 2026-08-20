package com.subsidytracker.integration.treasury;

import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementRequest;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TreasuryMockControllerTest {

    private TreasuryMockController mockController;

    @BeforeEach
    void setUp() {
        mockController = new TreasuryMockController();
    }

    @Test
    void disburse_ValidRequest_ShouldReturnSuccessResponse() {
        TreasuryDisbursementRequest request = new TreasuryDisbursementRequest(
                100L, 1L, 50L, new BigDecimal("10000"), "ACC-101", "Grant payout");

        ResponseEntity<TreasuryDisbursementResponse> response = mockController.disburse(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getTransactionId()).startsWith("TXN-TREASURY-100-");
        assertThat(response.getBody().getApplicationId()).isEqualTo(100L);
    }

    @Test
    void disburse_InvalidRequest_ShouldReturnFailureResponse() {
        TreasuryDisbursementRequest request = new TreasuryDisbursementRequest(
                null, 1L, 50L, BigDecimal.ZERO, "ACC-101", "Invalid grant payout");

        ResponseEntity<TreasuryDisbursementResponse> response = mockController.disburse(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getTransactionId()).isNull();
    }
}
