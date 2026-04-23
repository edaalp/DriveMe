package com.driveme.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for submitting a user-reported issue.
 */
@Data
public class ReportIssueRequest {

    @NotBlank(message = "Description cannot be blank")
    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    /** Optional — present when reported from a trip-complete screen. */
    private UUID tripId;
}
