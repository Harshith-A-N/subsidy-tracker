package com.subsidytracker.common.enums;

/**
 * Represents the possible decisions an officer can make during verification.
 */
public enum VerificationDecision {
    APPROVED,           // Officer approves — application moves to the next stage
    REJECTED,           // Officer rejects — workflow ends
    RE_VERIFICATION     // Officer requests re-verification — sent back for corrections
}
