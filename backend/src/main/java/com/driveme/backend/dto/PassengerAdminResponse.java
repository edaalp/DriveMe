package com.driveme.backend.dto;

import com.driveme.backend.common.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Admin-facing passenger detail (URLs point at authenticated document endpoints).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerAdminResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String userName;
    private boolean active;
    /** File name of uploaded identity document (ID card / passport), if any. */
    private String identityDocumentFileName;
    /** URL path for profile (selfie) when stored in DB. */
    private String profilePictureUrl;
    /** URL path for identity document when stored in DB. */
    private String identityDocumentUrl;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
}
