package com.driveme.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for price preview response.
 * Matches Flutter's TripPricePreview model exactly.
 * This is a non-binding price estimate shown to the user before creating the trip request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPricePreview {

    @JsonProperty("minPrice")
    private Double minPrice;

    @JsonProperty("maxPrice")
    private Double maxPrice;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("distanceKm")
    private Double distanceKm;

    // Optional: Can add extra fields that Flutter may use in the future
    @JsonProperty("durationMinutes")
    private Integer durationMinutes;
}

