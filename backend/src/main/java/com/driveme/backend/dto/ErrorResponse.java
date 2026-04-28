package com.driveme.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error response DTO for API endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("errorId")
    private String errorId;

    @JsonProperty("timestamp")
    private Long timestamp;
}

