package com.driveme.backend.dto;

import com.driveme.backend.common.TransmissionType;
import com.driveme.backend.common.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Vehicle responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {

    private UUID id;
    private String plateNumber;
    private String brand;
    private String model;
    private Integer year;
    private TransmissionType transmission;
    private VerificationStatus status;
    private UUID passengerId;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Returns formatted display name like "Toyota Camry 2020"
     */
    public String getDisplayName() {
        return String.format("%s %s %d", brand, model, year);
    }
}

