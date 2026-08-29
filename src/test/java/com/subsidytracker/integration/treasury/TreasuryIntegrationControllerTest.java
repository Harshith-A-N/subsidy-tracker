package com.subsidytracker.integration.treasury;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementRequest;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreasuryIntegrationControllerTest {

    @Mock
    private TreasuryClient treasuryClient;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TreasuryIntegrationController controller;

    private TreasuryDisbursementRequest request;
    private TreasuryDisbursementResponse response;

    @BeforeEach
    void setUp() {
        request = new TreasuryDisbursementRequest(
                100L, 1L, 50L, new BigDecimal("25000"), "ACC-999", "Stage release");

        response = new TreasuryDisbursementResponse(
                "TXN-999", 100L, true, "Disbursement accepted", "2026-08-19T22:30:00");
    }

    @Test
    void disburse_ShouldDelegateToTreasuryClientAndLogAudit() {
        when(treasuryClient.processDisbursement(request)).thenReturn(response);

        ResponseEntity<TreasuryDisbursementResponse> result = controller.disburse(request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTransactionId()).isEqualTo("TXN-999");
        verify(treasuryClient).processDisbursement(request);
        verify(auditLogService).logEvent(eq("TreasuryDisbursement"), eq(100L), eq("TREASURY_DISPATCHED"), eq((User) null), anyString());
    }
}
