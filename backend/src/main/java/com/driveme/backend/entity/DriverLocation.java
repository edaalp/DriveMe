package com.driveme.backend.entity;

import com.driveme.backend.common.BaseEntity;
import com.driveme.backend.common.Location;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity to track driver's current location for trip matching.
 * Stores the driver's last known location and when it was updated.
 */
@Entity
@Table(name = "driver_locations")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocation extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "lon")),
            @AttributeOverride(name = "addressText", column = @Column(name = "address_text"))
    })
    private Location currentLocation;

    @Column(nullable = false)
    private Instant lastUpdatedAt;
}

