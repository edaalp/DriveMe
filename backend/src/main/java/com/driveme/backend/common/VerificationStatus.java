package com.driveme.backend.common;

/**
 * Verification status for vehicles.
 */
public enum VerificationStatus {
    /**
     * Vehicle is pending verification.
     */
    PENDING,

    /**
     * Vehicle has been verified and approved.
     */
    VERIFIED,

    /**
     * Vehicle verification was rejected.
     */
    REJECTED
}

