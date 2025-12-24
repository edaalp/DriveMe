package com.driveme.backend.dto;

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
    private boolean available;
    private long tckNo;
    private String driverLicenseNumber;
    private Date licenseIssueDate;
    private String criminalRecordFileName;
}
