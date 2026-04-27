package com.driveme.backend.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO returned to the admin panel for a reported issue.
 */
@Data
public class ReportedIssueDTO {
    private UUID id;
    private UUID reporterId;
    private String reporterName;
    private String reporterRole;
    private UUID tripId;
    private String description;
    private boolean resolved;
    private Instant createdAt;
}
