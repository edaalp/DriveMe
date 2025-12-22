package com.driveme.requests.dto;

import java.time.Instant;

public record RideRequestEvent(
        Long requestId,
        Double pickupLat,
        Double pickupLng,
        Double destLat,
        Double destLng,
        Instant createdAt
) {}
