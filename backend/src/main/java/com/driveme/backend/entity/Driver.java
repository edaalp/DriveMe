package com.driveme.backend.entity;

import java.util.Date;

import com.driveme.backend.common.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Driver user type.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
@AllArgsConstructor
public class Driver extends BaseUser {

    private String licenseNumber;

    private String vehicleDescription;

    private boolean isAvailable;

    private double maxPickupRadiusKm;

    private double maxDropoffRadiusKm;

    private boolean acceptsPets;

    private double avgRating;

    private long tckNo;

    private String driverLicenseNumber;

    private Date licanseIssueDate;

    @Lob
    private byte[] criminalRecordFile;

    private String criminalRecordFileName;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;
}
