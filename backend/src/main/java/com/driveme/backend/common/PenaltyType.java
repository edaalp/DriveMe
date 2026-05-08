package com.driveme.backend.common;

/**
 * Enum representing the type of penalty applied to a user.
 */
public enum PenaltyType {
    
    /**
     * Penalty for cancelling a trip.
     */
    CANCELLATION,

    /**
     * Penalty for a passenger not showing up after the driver arrived.
     */
    NO_SHOW,
    
    /**
     * Penalty for speeding violations.
     */
    SPEED
}

