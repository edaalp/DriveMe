package com.driveme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Data Transfer Object for driver sign-up request.
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

    private long tckNo;

    private String driverLicenseNumber;

    private Date licenseIssueDate;

    // Base64 encoded criminal record file
    private String criminalRecordFile;

    private String criminalRecordFileName;
}
