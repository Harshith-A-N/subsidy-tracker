package com.subsidytracker.common.enums;

public enum DocumentVerificationStatus {
    PENDING,   // uploaded, not yet checked by field officer
    VERIFIED,  // field officer confirmed it's genuine/correct
    REJECTED   // field officer found it invalid/unclear
}