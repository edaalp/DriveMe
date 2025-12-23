package com.driveme.backend.penalty.dto;

import com.driveme.backend.penalty.PenaltyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a new penalty.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePenaltyRequest {

    /**
     * The ID of the user to apply the penalty to.
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    /**
     * The type of penalty.
     */
    @NotNull(message = "Penalty type is required")
    private PenaltyType type;

    /**
     * The penalty amount.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * The currency code (default: TRY).
     */
    private String currency = "TRY";

    /**
     * The reason for the penalty.
     */
    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * Optional trip ID if penalty is related to a trip.
     */
    private UUID tripId;
}

