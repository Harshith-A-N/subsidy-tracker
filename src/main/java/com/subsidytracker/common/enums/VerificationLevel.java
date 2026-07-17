package com.subsidytracker.common.enums;

/**
 * Represents the three levels of the multi-level verification workflow.
 * An application moves through these levels in order: FIELD → DISTRICT → FINANCE.
 */
public enum VerificationLevel {
    FIELD,      // Field Officer verification (ground-level check)
    DISTRICT,   // District Officer verification (supervisory review)
    FINANCE     // Finance Approver verification (budget and final approval)
}
