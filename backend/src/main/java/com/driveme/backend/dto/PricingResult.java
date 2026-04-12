package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value object representing the result of pricing calculation.
 * Used internally by pricing services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingResult {

    /**
     * Distance in kilometers
     */
    private double distanceKm;

    /**
     * Estimated duration in minutes
     */
    private int durationMinutes;

    /**
     * Base price before multipliers (distance + duration based)
     */
    private double basePrice;

    /**
     * Surge/demand-supply multiplier
     */
    private double surgeMultiplier;

    /**
     * Time-based multiplier (rush hour, night, etc.)
     */
    private double timeMultiplier;

    /**
     * Weather-based multiplier
     */
    private double weatherMultiplier;

    /**
     * Final base price after all multipliers
     */
    private double finalBasePrice;

    /**
     * Minimum price (final base price * 0.9)
     */
    private double minPrice;

    /**
     * Maximum price (final base price * 1.1)
     */
    private double maxPrice;

    /**
     * Currency code (e.g., "TRY")
     */
    private String currency;
}

