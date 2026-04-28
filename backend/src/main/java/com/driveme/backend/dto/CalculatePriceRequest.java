package com.driveme.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for price calculation endpoint.
 * Sent by Flutter app to get price preview before creating a trip request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculatePriceRequest {

    @NotNull(message = "Pickup location is required")
    @Valid
    private LocationDTO pickup;

    @NotNull(message = "Destination location is required")
    @Valid
    private LocationDTO destination;
}

