package com.driveme.backend.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional body for trip completion. When the driver's app tracks the real
 * route it can report the measured distance / duration; otherwise these stay
 * null and the trip keeps only the planned values.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTripRequest {

    @Positive(message = "Actual distance must be positive")
    private Double actualDistanceKm;

    @Positive(message = "Actual duration must be positive")
    private Integer actualDurationMinutes;

    /** Encoded Google polyline of the driven route (optional, for post-trip map display). */
    private String routePolyline;
}
