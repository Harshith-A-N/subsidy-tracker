package com.subsidytracker.integration.treasury;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementRequest;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/treasury")
public class TreasuryIntegrationController {

    private static final Logger logger = LoggerFactory.getLogger(TreasuryIntegrationController.class);

    private final TreasuryClient treasuryClient;
    private final AuditLogService auditLogService;

    public TreasuryIntegrationController(
            TreasuryClient treasuryClient,
            AuditLogService auditLogService) {

        this.treasuryClient = treasuryClient;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/disburse")
    public ResponseEntity<TreasuryDisbursementResponse> disburse(
            @RequestBody TreasuryDisbursementRequest request) {

        TreasuryDisbursementResponse response = treasuryClient.processDisbursement(request);

        try {
            auditLogService.logEvent(
                    "TreasuryDisbursement",
                    request.getApplicationId(),
                    "TREASURY_DISPATCHED",
                    (User) null,
                    "Disbursement of " + request.getAmount() + " dispatched to treasury. Transaction ID: " + response.getTransactionId());
        } catch (Exception e) {
            logger.warn("Failed to log audit event [entityType=TreasuryDisbursement, applicationId={}, action=TREASURY_DISPATCHED]: {}",
                    request.getApplicationId(), e.getMessage(), e);
        }

        return ResponseEntity.ok(response);
    }
}
