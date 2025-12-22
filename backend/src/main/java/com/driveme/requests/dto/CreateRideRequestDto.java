package com.driveme.requests.dto;

import jakarta.validation.constraints.NotNull;

public record CreateRideRequestDto(
        @NotNull Double pickupLat,
        @NotNull Double pickupLng,
        @NotNull Double destLat,
        @NotNull Double destLng
) {}
