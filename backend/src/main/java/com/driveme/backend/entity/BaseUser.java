package com.driveme.backend.entity;

import com.driveme.backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Abstract user with shared identity and contact fields.
 *
 * <p>{@link #avgRating} and {@link #ratingCount} are declared here rather
 * than on subclasses because both {@code Driver} and {@code Passenger} can
 * be rated after a completed trip. With SINGLE_TABLE inheritance they share
 * the same physical column, so duplicating them on subclasses would cause
 * column-mapping conflicts.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
@AllArgsConstructor
public abstract class BaseUser extends BaseEntity {

    private String email;
    
    private String fullName;

    private String userName;

    private String phoneNumber;

    private boolean active = true;

    private String passwordHash;

    /** Average rating (1.0–5.0) aggregated from counter-party ratings on completed trips. */
    @Column(name = "avg_rating")
    private Double avgRating;

    /** Number of ratings that contributed to {@link #avgRating}. */
    @Column(name = "rating_count")
    private Integer ratingCount;

    protected BaseUser() {
        // for JPA
    }
}
