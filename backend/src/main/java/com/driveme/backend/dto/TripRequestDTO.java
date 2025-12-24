package com.driveme.backend.dto;

import com.driveme.backend.common.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for TripRequest responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRequestDTO {

    private UUID id;
    private Instant requestedTime;
    private boolean withPet;
    private RequestStatus status;

    // Price range
    private BigDecimal minPriceAmount;
    private String minPriceCurrency;
    private BigDecimal maxPriceAmount;
    private String maxPriceCurrency;

    // Locations
    private LocationDTO pickup;
    private LocationDTO destination;

    // Distance in km
    private double distanceKm;

    // Related entities
    private UUID passengerId;
    private VehicleDTO vehicle;

    private Instant createdAt;
    private Instant updatedAt;
}

