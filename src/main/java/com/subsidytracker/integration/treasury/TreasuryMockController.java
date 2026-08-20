package com.subsidytracker.integration.treasury;

import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementRequest;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/mock/external-treasury")
public class TreasuryMockController {

    @PostMapping("/disburse")
    public ResponseEntity<TreasuryDisbursementResponse> disburse(
            @RequestBody TreasuryDisbursementRequest request) {

        if (request.getAmount() != null && request.getAmount().signum() > 0 && request.getApplicationId() != null) {
            String transactionId = "TXN-TREASURY-" + request.getApplicationId() + "-" + System.currentTimeMillis();
            return ResponseEntity.ok(
                    new TreasuryDisbursementResponse(
                            transactionId,
                            request.getApplicationId(),
                            true,
                            "Disbursement order accepted by State Treasury",
                            LocalDateTime.now().toString()
                    )
            );
        }

        return ResponseEntity.ok(
                new TreasuryDisbursementResponse(
                        null,
                        request != null ? request.getApplicationId() : null,
                        false,
                        "Invalid disbursement amount or missing application ID",
                        LocalDateTime.now().toString()
                )
        );
    }
}
