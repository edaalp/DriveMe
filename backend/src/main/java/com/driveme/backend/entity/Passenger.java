package com.driveme.backend.entity;

import com.driveme.backend.common.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Passenger user type.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
public class Passenger extends BaseUser {

    /** Turkish national ID number (TC Kimlik No). */
    private Long tcNo;

    /** Verification status for passenger approval workflow. */
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /** Reason for rejection, if applicable. */
    @Column(length = 1024)
    private String rejectionReason;

    /** URL of the front-side TC identity card photo, stored under /uploads/. */
    @Column(length = 512)
    private String tcPhotoFrontUrl;

    /** URL of the back-side TC identity card photo, stored under /uploads/. */
    @Column(length = 512)
    private String tcPhotoBackUrl;

    /** URL of the selfie used as the profile picture, stored under /uploads/. */
    @Column(length = 512)
    private String profilePictureUrl;

    // Database-stored file bytes for sharing with coworkers
    @Lob
    private byte[] tcPhotoFrontFile;

    private String tcPhotoFrontFileName;

    @Lob
    private byte[] tcPhotoBackFile;

    private String tcPhotoBackFileName;

    @Lob
    private byte[] profilePictureFile;

    private String profilePictureFileName;

    public Passenger() {
        super();
    }
}
