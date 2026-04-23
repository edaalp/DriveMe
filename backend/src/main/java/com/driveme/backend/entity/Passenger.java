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
@EqualsAndHashCode(callSuper = true)
public class Passenger extends BaseUser {

    @Lob
    private byte[] profilePictureFile;

    private String profilePictureFileName;

    @Lob
    private byte[] identityDocumentFile;

    private String identityDocumentFileName;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    public Passenger() {
        super();
    }
}
