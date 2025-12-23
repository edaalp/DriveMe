package com.driveme.backend.payment.dto;

import com.driveme.backend.payment.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for creating a new payment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    /**
     * The ID of the trip this payment is for.
     */
    private UUID tripId;

    /**
     * The ID of the user making the payment.
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    /**
     * The payment amount.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * The currency code (default: TRY).
     */
    private String currency = "TRY";

    /**
     * The payment method.
     */
    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    /**
     * Optional due date for the payment.
     */
    private Instant dueBy;

    /**
     * Optional description for the payment.
     */
    private String description;
}

