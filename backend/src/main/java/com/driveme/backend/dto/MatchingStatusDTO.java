package com.driveme.backend.dto;

import com.driveme.backend.common.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for trip request matching status.
 * Used to show passenger the current state of their trip request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingStatusDTO {

    private UUID tripId;
    private RequestStatus status;

    // Matching counters
    private int notifiedDriversCount;
    private int viewingDriversCount;
    private int respondedDriversCount;

    // When matched
    private DriverInfoDTO matchedDriver;

    /**
     * Simple driver info for matching status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfoDTO {
        private UUID id;
        private String fullName;
        private double avgRating;
        private int etaMinutes;
        private String vehicleDescription;
        private boolean acceptsPets;
    }
}

