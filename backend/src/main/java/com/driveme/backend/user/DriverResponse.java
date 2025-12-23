package com.driveme.backend.user;

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
    private String vehicleDescription;
    private boolean available;
    private double maxPickupRadiusKm;
    private double maxDropoffRadiusKm;
    private boolean acceptsPets;
    private double avgRating;
    private long tckNo;
    private String driverLicenseNumber;
    private Date licenseIssueDate;
    private String criminalRecordFileName;
}
