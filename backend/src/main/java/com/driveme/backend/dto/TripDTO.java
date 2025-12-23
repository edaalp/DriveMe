package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for trip responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripDTO {
    private UUID passengerId;
    private UUID driverId;
    private Timestamp startTime;
    private Timestamp endTime;
    private String status;

}

