package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for available trip requests visible to drivers.
 * Contains all information a driver needs to decide whether to accept a trip.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTripRequestDTO {

    private UUID tripId;

    /**
     * When the passenger posted this request. Clients display it as
     * "X minutes ago" in the driver's list view.
     */
    private Instant requestedTime;

    // Passenger info
    private String passengerName;
    private double passengerRating;
    private long passengerTripCount;

    // Trip details
    private boolean withPet;
    private LocationDTO pickup;
    private LocationDTO destination;

    // Distance and pricing
    private double distanceKm;
    private long estimatedMinutes;
    private BigDecimal offerAmount;
    private String offerCurrency;

    // Vehicle info (if available)
    private VehicleDTO vehicle;
}

