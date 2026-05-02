package com.driveme.backend.dto;

import com.driveme.backend.common.TransmissionType;
import com.driveme.backend.common.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
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
    private String rejectionReason;
    /** True when at least one vehicle document exists (derived). */
    private boolean hasDocument;
    private List<VehicleDocumentDTO> documents;
    private UUID passengerId;
    private String ownerFullName;
    private String ownerEmail;
    private String ownerPhoneNumber;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Returns formatted display name like "Toyota Camry 2020"
     */
    public String getDisplayName() {
        return String.format("%s %s %d", brand, model, year);
    }
}

