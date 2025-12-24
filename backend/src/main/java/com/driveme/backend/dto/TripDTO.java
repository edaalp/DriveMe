package com.driveme.backend.dto;

import com.driveme.backend.entity.TripStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for trip responses.
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripDTO {
    private UUID passengerId;
    private UUID driverId;
    private TripStatusEnum status;
    private Timestamp startTime;
    private Timestamp endTime;
    private String status;

}

