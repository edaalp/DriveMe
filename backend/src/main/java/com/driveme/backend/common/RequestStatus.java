package com.driveme.backend.common;

/**
 * Status of a trip request.
 */
public enum RequestStatus {
    /**
     * Request is active and waiting for driver offers.
     */
    PENDING,

    /**
     * Request has been matched with a driver.
     */
    MATCHED,

    /**
     * Request was cancelled by the passenger.
     */
    CANCELLED,

    /**
     * Request expired without being matched.
     */
    EXPIRED
}

