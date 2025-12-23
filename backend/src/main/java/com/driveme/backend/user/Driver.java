package com.driveme.backend.user;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Driver user type.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
@AllArgsConstructor
public class Driver extends BaseUser {

    private String licenseNumber;

    private String vehicleDescription;

    private boolean isAvailable;

    private double maxPickupRadiusKm;

    private double maxDropoffRadiusKm;

    private boolean acceptsPets;

    private double avgRating;

    private int tckNo;

    private String DriverLicenseNumber;

    private Date licanseIssueDate;

    @Lob
    private byte[] criminalRecordFile;

    private String criminalRecordFileName;

    protected Driver() {
        // for JPA
    }

}
