package com.driveme.backend.entity;

import com.driveme.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track driver rejections of trip requests.
 * Used to prevent showing the same rejected trip to a driver multiple times.
 */
@Entity
@Table(name = "driver_trip_rejections", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"trip_request_id", "driver_id"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class DriverTripRejection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_request_id", nullable = false)
    private TripRequest tripRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(nullable = false, updatable = false)
    private Instant rejectedAt;
}

