package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Partial update for the authenticated driver profile (PATCH /api/drivers/me).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverMePatchRequest {

    /** When non-null, updates whether the driver accepts pet trips. */
    private Boolean acceptsPets;
}
