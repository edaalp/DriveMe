package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for rating responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingDTO {
    private UUID ratingId;
    private UUID raterId;
    private UUID rateeId;
    private int score;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}

