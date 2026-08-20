package com.subsidytracker.integration.treasury;

import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementRequest;
import com.subsidytracker.integration.treasury.dto.TreasuryDisbursementResponse;

public interface TreasuryClient {

    TreasuryDisbursementResponse processDisbursement(TreasuryDisbursementRequest request);
}
