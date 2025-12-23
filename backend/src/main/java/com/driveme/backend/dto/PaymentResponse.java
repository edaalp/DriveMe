package com.driveme.backend.dto;

import com.driveme.backend.common.PaymentMethod;
import com.driveme.backend.common.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for payment response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID tripId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private PaymentStatus status;
    private Instant dueBy;
    private Instant confirmedAt;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean overdue;
}

