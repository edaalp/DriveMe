package com.driveme.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for creating a new trip request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequestRequest {

    @NotNull(message = "Requested time is required")
    private Instant requestedTime;

    private boolean withPet = false;

    @NotNull(message = "Pickup location is required")
    @Valid
    private LocationDTO pickup;

    @NotNull(message = "Destination location is required")
    @Valid
    private LocationDTO destination;

    /**
     * Optional: Vehicle ID if passenger wants to use their own vehicle.
     */
    private UUID vehicleId;
}

