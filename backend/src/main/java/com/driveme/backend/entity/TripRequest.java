package com.driveme.backend.entity;

import com.driveme.backend.common.BaseEntity;
import com.driveme.backend.common.Location;
import com.driveme.backend.common.Money;
import com.driveme.backend.common.RequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * TripRequest entity representing a passenger's request for a driver.
 */
@Entity
@Table(name = "trip_requests")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class TripRequest extends BaseEntity {

    @Column(nullable = false)
    private Instant requestedTime;

    @Column(nullable = false)
    private boolean withPet = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "min_price_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "min_price_currency"))
    })
    private Money minPrice;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "max_price_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "max_price_currency"))
    })
    private Money maxPrice;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "pickup_lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "pickup_lon")),
            @AttributeOverride(name = "addressText", column = @Column(name = "pickup_address"))
    })
    private Location pickup;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "destination_lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "destination_lon")),
            @AttributeOverride(name = "addressText", column = @Column(name = "destination_address"))
    })
    private Location destination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    /**
     * Calculate distance between pickup and destination in kilometers.
     */
    public double getDistanceKm() {
        if (pickup == null || destination == null) {
            return 0;
        }
        return pickup.distanceTo(destination);
    }

    /**
     * Cancel this trip request.
     */
    public void cancel() {
        if (this.status == RequestStatus.PENDING) {
            this.status = RequestStatus.CANCELLED;
        } else {
            throw new IllegalStateException("Cannot cancel a request that is not pending");
        }
    }

    /**
     * Expire this trip request.
     */
    public void expire() {
        if (this.status == RequestStatus.PENDING) {
            this.status = RequestStatus.EXPIRED;
        }
    }

    /**
     * Mark this request as matched with a driver.
     */
    public void markMatched() {
        if (this.status == RequestStatus.PENDING) {
            this.status = RequestStatus.MATCHED;
        } else {
            throw new IllegalStateException("Cannot match a request that is not pending");
        }
    }
}

