package com.driveme.backend.dto;

import com.driveme.backend.common.PenaltyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for penalty response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyResponse {

    private UUID id;
    private UUID userId;
    private PenaltyType type;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private boolean paid;
    private UUID tripId;
    private UUID paymentId;
    private Instant createdAt;
    private Instant updatedAt;
}

