package com.driveme.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Periodic driver location update while en route to pickup. The driver's
 * app is expected to post this every few seconds; the passenger's waiting
 * screen polls {@code GET /trips/{id}} to render a live map.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationPing {

    @NotNull
    @Min(value = -90,  message = "Latitude must be between -90 and 90")
    @Max(value =  90,  message = "Latitude must be between -90 and 90")
    private Double lat;

    @NotNull
    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value =  180, message = "Longitude must be between -180 and 180")
    private Double lon;

    /** Optional ETA to pickup in minutes, as computed by the driver's on-device routing. */
    @PositiveOrZero(message = "ETA must be non-negative")
    private Integer etaToPickupMinutes;
}
