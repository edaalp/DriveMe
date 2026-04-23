package com.driveme.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    /** URL of the front-side TC identity card photo, stored under /uploads/. */
    @Column(length = 512)
    private String tcPhotoFrontUrl;

    /** URL of the back-side TC identity card photo, stored under /uploads/. */
    @Column(length = 512)
    private String tcPhotoBackUrl;

    /** URL of the selfie used as the profile picture, stored under /uploads/. */
    @Column(length = 512)
    private String profilePictureUrl;

    public Passenger() {
        super();
    }
}
