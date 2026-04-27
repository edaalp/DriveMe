package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for trip request accept/reject responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripAcceptanceResponseDTO {

    private UUID tripId;

    /**
     * ID of the freshly-created {@code Trip} entity spawned from this acceptance.
     * Use this to hit {@code /api/trips/{id}} for the confirmed ride view.
     */
    private UUID tripEntityId;

    private String message;
    private boolean success;

    /**
     * For accept - includes matched trip details.
     */
    private TripRequestDTO tripDetails;

    /**
     * For accept - driver confirmation.
     */
    private DriverInfoDTO driverInfo;

    /**
     * Simple driver info for responses.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfoDTO {
        private UUID id;
        private String fullName;
        private String phoneNumber;
        private double avgRating;
    }
}

