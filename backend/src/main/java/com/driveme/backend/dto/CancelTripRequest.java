package com.driveme.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional cancellation reason supplied by the cancelling party.
 * The body itself is optional on the wire — sending an empty body still
 * cancels the trip but without a recorded reason.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelTripRequest {

    @Size(max = 500, message = "Reason must be 500 characters or fewer")
    private String reason;
}
