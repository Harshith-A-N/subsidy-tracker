package com.subsidytracker.common.enums;

public enum ApplicationStatus {
    SUBMITTED,              // just applied, nothing reviewed yet
    ELIGIBILITY_SCORED,     // automated scoring done, waiting to enter verification
    PENDING_FIELD_REVIEW,   // sitting with field officer
    PENDING_DISTRICT_REVIEW,// field officer approved, now with district officer
    PENDING_FINANCE_REVIEW, // district approved, now with finance approver
    APPROVED,               // finance approved — cleared for disbursement
    REJECTED,               // rejected at some stage
    RE_VERIFICATION_REQUESTED, // sent back for re-check instead of outright rejection
    DISBURSEMENT_IN_PROGRESS,  // approved, staged disbursement has started but not finished
    DISBURSEMENT_COMPLETED
}
