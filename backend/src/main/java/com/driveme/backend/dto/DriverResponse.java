package com.driveme.backend.dto;

import com.driveme.backend.common.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Data Transfer Object for driver response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private boolean active;
    private String licenseNumber;
    /** Free-text vehicle description when the driver has registered vehicle details. */
    private String vehicleDescription;
    private boolean available;
    /** Whether this driver accepts trip requests that include a pet. */
    private boolean acceptsPets;
    private long tckNo;
    private String driverLicenseNumber;
    private Date licenseIssueDate;
    private String criminalRecordFileName;
    /** URL path (e.g. {@code /uploads/drivers/uuid.pdf}) for the license document. */
    private String driverLicenseDocumentUrl;
    /** URL path for the criminal record document. */
    private String criminalRecordDocumentUrl;
    /** Profile (selfie) image URL. */
    private String profilePictureUrl;
    /** Visible account rating shown as stars. New accounts start at 5.0. */
    private Double avgRating;
    private Integer ratingCount;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
}
