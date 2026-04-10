package com.driveme.backend.dto;

import com.driveme.backend.common.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin verification decisions (approve/reject).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDecisionRequest {

    @NotNull(message = "Decision is required (VERIFIED or REJECTED)")
    private VerificationStatus decision;

    /**
     * Required when decision is REJECTED.
     */
    private String reason;
}
