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
    private long tckNo;
    private String driverLicenseNumber;
    private Date licenseIssueDate;
    private String criminalRecordFileName;
    /** URL path (e.g. {@code /uploads/drivers/uuid.pdf}) for the license document. */
    private String driverLicenseDocumentUrl;
    /** URL path for the criminal record document. */
    private String criminalRecordDocumentUrl;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
}
