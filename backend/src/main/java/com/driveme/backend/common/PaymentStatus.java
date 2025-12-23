package com.driveme.backend.common;

/**
 * Enum representing the status of a payment.
 */
public enum PaymentStatus {
    
    /**
     * Payment is awaiting confirmation.
     */
    PENDING,
    
    /**
     * Payment has been confirmed successfully.
     */
    CONFIRMED,
    
    /**
     * Payment has failed or been cancelled.
     */
    FAILED,
    
    /**
     * Payment has been cancelled by user or system.
     */
    CANCELLED
}

