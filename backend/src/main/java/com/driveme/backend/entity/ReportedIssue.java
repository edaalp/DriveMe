package com.driveme.backend.entity;

import com.driveme.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity representing an issue reported by a passenger or driver.
 */
@Entity
@Table(name = "reported_issues")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ReportedIssue extends BaseEntity {

    /**
     * ID of the user who submitted the report (passenger or driver).
     */
    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    /**
     * Full name of the reporter, stored at submission time.
     */
    @Column(name = "reporter_name", nullable = false, length = 200)
    private String reporterName;

    /**
     * "PASSENGER" or "DRIVER"
     */
    @Column(name = "reporter_role", nullable = false, length = 20)
    private String reporterRole;

    /**
     * Optional trip ID when the issue is trip-specific.
     */
    @Column(name = "trip_id")
    private UUID tripId;

    /**
     * The issue text submitted by the user.
     */
    @Column(nullable = false, length = 2000)
    private String description;

    /**
     * Whether the admin has reviewed/resolved this issue.
     */
    @Column(nullable = false)
    private boolean resolved = false;
}
