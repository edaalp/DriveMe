package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Data Transfer Object for driver sign-up request (JSON part of multipart signup).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverSignUpRequest {

    private String email;

    private String fullName;

    private String phoneNumber;

    private String password;

    private String confirmPassword;

    private String licenseNumber;

    /** Optional; server applies defaults when null. */
    private String vehicleDescription;

    private Double maxPickupRadiusKm;

    private Double maxDropoffRadiusKm;

    private Boolean acceptsPets;

    private long tckNo;

    private String driverLicenseNumber;

    private Date licenseIssueDate;

    /** Original filename of the license upload (optional; also sent as multipart file). */
    private String driverLicenseFileName;

    /** Original filename of the criminal record upload (optional; also sent as multipart file). */
    private String criminalRecordFileName;
}
