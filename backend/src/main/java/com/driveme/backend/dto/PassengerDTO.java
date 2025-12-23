package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for passenger response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerDTO {

    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private boolean active;
}
