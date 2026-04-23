package com.driveme.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for rating a completed trip. Reused by both the passenger-rates-driver
 * and the driver-rates-passenger endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateTripRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 500, message = "Comment must be 500 characters or fewer")
    private String comment;
}
