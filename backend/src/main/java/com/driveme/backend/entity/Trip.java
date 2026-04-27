package com.driveme.backend.entity;

import com.driveme.backend.common.BaseEntity;
import com.driveme.backend.common.Location;
import com.driveme.backend.common.Money;
import com.driveme.backend.common.PaymentMethod;
import com.driveme.backend.common.PaymentStatus;
import com.driveme.backend.common.TripStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * A confirmed ride between a driver and a passenger.
 *
 * <p>Conceptually distinct from {@link TripRequest}:
 * <ul>
 *   <li>{@code TripRequest} is the passenger's <em>posted offer</em>. It ends
 *   its lifecycle the moment it is matched, cancelled or expires.</li>
 *   <li>{@code Trip} is the <em>binding agreement</em> between the matched
 *   parties. It captures the lifecycle from acceptance all the way through to
 *   rating and payment.</li>
 * </ul>
 *
 * <h3>Snapshot semantics</h3>
 * <p>Pickup/destination, the agreed price, pet preference and the chosen
 * vehicle are <strong>snapshotted</strong> onto the Trip at creation time.
 * The passenger can delete their {@code TripRequest} (or sell the vehicle) and
 * a driver's trip history stays intact.
 *
 * <h3>State machine</h3>
 * <pre>
 *   ACCEPTED ─┬─► DRIVER_ARRIVED ─┬─► IN_PROGRESS ──► COMPLETED
 *             │                    │
 *             │                    └─► NO_SHOW
 *             │
 *             ├─► CANCELLED_BY_DRIVER
 *             └─► CANCELLED_BY_PASSENGER
 * </pre>
 */
@Entity
@Table(name = "trips", indexes = {
        @Index(name = "ix_trips_driver_status",    columnList = "driver_id,status"),
        @Index(name = "ix_trips_passenger_status", columnList = "passenger_id,status"),
        @Index(name = "ix_trips_status",           columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Trip extends BaseEntity {

    // ── Relationships ────────────────────────────────────────────────────────

    /** The request this trip originated from. Preserved for audit/traceability. */
    @Column(name = "trip_request_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID tripRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    // ── Route (snapshot) ─────────────────────────────────────────────────────

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat",         column = @Column(name = "pickup_lat")),
            @AttributeOverride(name = "lon",         column = @Column(name = "pickup_lon")),
            @AttributeOverride(name = "addressText", column = @Column(name = "pickup_address"))
    })
    private Location pickup;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat",         column = @Column(name = "destination_lat")),
            @AttributeOverride(name = "lon",         column = @Column(name = "destination_lon")),
            @AttributeOverride(name = "addressText", column = @Column(name = "destination_address"))
    })
    private Location destination;

    @Column(name = "planned_distance_km")
    private Double plannedDistanceKm;

    @Column(name = "planned_duration_minutes")
    private Integer plannedDurationMinutes;

    @Column(name = "actual_distance_km")
    private Double actualDistanceKm;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    /** Encoded Google polyline of the driven route, for post-trip map display. */
    @Column(name = "route_polyline", length = 4096)
    private String routePolyline;

    // ── Pricing & payment ────────────────────────────────────────────────────

    /** Locked-in price agreed at acceptance time (passenger's offer). */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",   column = @Column(name = "agreed_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "agreed_currency"))
    })
    private Money agreedAmount;

    /** Final amount if the price was adjusted (e.g. waiting fee, penalty). */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",   column = @Column(name = "final_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "final_currency"))
    })
    private Money finalAmount;

    /** Penalty charged if one side cancelled after acceptance. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",   column = @Column(name = "penalty_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "penalty_currency"))
    })
    private Money penaltyAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    // ── Preferences (snapshot) ───────────────────────────────────────────────

    @Column(name = "with_pet", nullable = false)
    private boolean withPet;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripStatus status = TripStatus.ACCEPTED;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "driver_arrived_at")
    private Instant driverArrivedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    // ── Boarding verification ────────────────────────────────────────────────

    /**
     * 4-digit code generated when the driver marks arrival at the pickup point.
     * The driver reads it aloud; the passenger confirms it in the app to start
     * the trip (DRIVER_ARRIVED → IN_PROGRESS transition).
     */
    @Column(name = "verification_code", length = 10)
    private String verificationCode;

    // ── Live tracking (optional) ─────────────────────────────────────────────

    /** Last reported driver latitude while en route (updated periodically). */
    @Column(name = "driver_last_lat")
    private Double driverLastLat;

    /** Last reported driver longitude while en route. */
    @Column(name = "driver_last_lon")
    private Double driverLastLon;

    /** Driver's last-known ETA to pickup, in minutes. Null once onboard. */
    @Column(name = "eta_to_pickup_minutes")
    private Integer etaToPickupMinutes;

    // ── Ratings & feedback ───────────────────────────────────────────────────

    /** Rating the passenger gave to the driver (1-5). Null until submitted. */
    @Column(name = "rating_by_passenger")
    private Integer ratingByPassenger;

    /** Rating the driver gave to the passenger (1-5). Null until submitted. */
    @Column(name = "rating_by_driver")
    private Integer ratingByDriver;

    @Column(name = "comment_by_passenger", length = 500)
    private String commentByPassenger;

    @Column(name = "comment_by_driver", length = 500)
    private String commentByDriver;

    // ── State-transition helpers ─────────────────────────────────────────────

    /** Marks the driver as having arrived at pickup. Only valid from {@code ACCEPTED}. */
    public void markDriverArrived() {
        requireStatus(TripStatus.ACCEPTED, "driver arrived");
        this.status = TripStatus.DRIVER_ARRIVED;
        this.driverArrivedAt = Instant.now();
        this.etaToPickupMinutes = 0;
    }

    /** Starts the actual ride. Valid from {@code ACCEPTED} or {@code DRIVER_ARRIVED}. */
    public void start() {
        if (this.status != TripStatus.ACCEPTED && this.status != TripStatus.DRIVER_ARRIVED) {
            throw new IllegalStateException(
                    "Cannot start trip from status " + this.status);
        }
        this.status = TripStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        this.etaToPickupMinutes = null;
    }

    /** Marks the trip as completed. Valid only from {@code IN_PROGRESS}. */
    public void complete(Double actualDistanceKm, Integer actualDurationMinutes) {
        requireStatus(TripStatus.IN_PROGRESS, "complete");
        this.status = TripStatus.COMPLETED;
        this.completedAt = Instant.now();
        if (actualDistanceKm != null) {
            this.actualDistanceKm = actualDistanceKm;
        }
        if (actualDurationMinutes != null) {
            this.actualDurationMinutes = actualDurationMinutes;
        }
        // Cash default: settlement happens at the end of the ride.
        if (this.paymentStatus == PaymentStatus.PENDING
                && this.paymentMethod == PaymentMethod.CASH) {
            this.paymentStatus = PaymentStatus.CONFIRMED;
        }
    }

    /** Cancels the trip on behalf of the given party. Not allowed once the ride has started. */
    public void cancelBy(CancelledBy party, String reason) {
        if (this.status == TripStatus.IN_PROGRESS || this.status == TripStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cannot cancel a trip that is " + this.status);
        }
        this.status = party == CancelledBy.DRIVER
                ? TripStatus.CANCELLED_BY_DRIVER
                : TripStatus.CANCELLED_BY_PASSENGER;
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
    }

    /** Marks the passenger as a no-show. Only valid from {@code DRIVER_ARRIVED}. */
    public void markNoShow() {
        requireStatus(TripStatus.DRIVER_ARRIVED, "no-show");
        this.status = TripStatus.NO_SHOW;
        this.cancelledAt = Instant.now();
        this.cancellationReason = "Passenger did not show up at pickup";
    }

    /** Convenience: is the trip in a terminal state? */
    public boolean isFinished() {
        return switch (this.status) {
            case COMPLETED, CANCELLED_BY_DRIVER, CANCELLED_BY_PASSENGER, NO_SHOW -> true;
            default -> false;
        };
    }

    private void requireStatus(TripStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " from status " + this.status + " (expected " + expected + ")");
        }
    }

    /** Which side cancelled the trip — used only by {@link #cancelBy(CancelledBy, String)}. */
    public enum CancelledBy { DRIVER, PASSENGER }
}
