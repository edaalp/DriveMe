package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for price range calculation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRangeDTO {

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String currency;
    private double distanceKm;

    /**
     * Creates a TRY price range.
     */
    public static PriceRangeDTO ofTRY(BigDecimal minPrice, BigDecimal maxPrice, double distanceKm) {
        return PriceRangeDTO.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .currency("TRY")
                .distanceKm(distanceKm)
                .build();
    }
}

