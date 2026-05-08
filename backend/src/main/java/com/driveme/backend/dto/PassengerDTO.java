package com.driveme.backend.dto;

import com.driveme.backend.common.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for passenger response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerDTO {

    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private boolean active;
    private Long tcNo;
    private String tcPhotoFrontUrl;
    private String tcPhotoBackUrl;
    private String profilePictureUrl;
    /** Visible account rating shown as stars. New accounts start at 5.0. */
    private Double avgRating;
    private Integer ratingCount;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
}
