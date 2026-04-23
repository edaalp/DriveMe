package com.driveme.backend.dto;

import com.driveme.backend.common.PaymentMethod;
import com.driveme.backend.common.PaymentStatus;
import com.driveme.backend.common.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Full DTO projection of a {@code Trip} — used by both driver and passenger
 * clients. All fields are optional so a single shape works for any phase of
 * the trip lifecycle (e.g. {@code actualDistanceKm} is null until completion,
 * ratings are null until submitted).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDTO {

    // ── Identity ─────────────────────────────────────────────────────────────
    private UUID id;
    private UUID tripRequestId;
    private TripStatus status;

    // ── Parties (denormalised summary, not full entity graphs) ───────────────
    private PartyDTO driver;
    private PartyDTO passenger;
    private VehicleDTO vehicle;

    // ── Route (snapshot) ─────────────────────────────────────────────────────
    private LocationDTO pickup;
    private LocationDTO destination;

    private Double plannedDistanceKm;
    private Integer plannedDurationMinutes;
    private Double actualDistanceKm;
    private Integer actualDurationMinutes;
    private String routePolyline;

    // ── Pricing & payment ────────────────────────────────────────────────────
    private BigDecimal agreedAmount;
    private String    agreedCurrency;
    private BigDecimal finalAmount;
    private String    finalCurrency;
    private BigDecimal penaltyAmount;
    private String    penaltyCurrency;

    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    // ── Preferences ──────────────────────────────────────────────────────────
    private boolean withPet;

    // ── Lifecycle timestamps ─────────────────────────────────────────────────
    private Instant acceptedAt;
    private Instant driverArrivedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String  cancellationReason;

    // ── Live tracking ────────────────────────────────────────────────────────
    private Double  driverLastLat;
    private Double  driverLastLon;
    private Integer etaToPickupMinutes;

    // ── Boarding verification ─────────────────────────────────────────────────
    /** 4-digit code shown to the driver and entered by the passenger to start the trip. */
    private String verificationCode;

    // ── Ratings ──────────────────────────────────────────────────────────────
    private Integer ratingByPassenger;
    private Integer ratingByDriver;
    private String  commentByPassenger;
    private String  commentByDriver;

    // ── Audit ────────────────────────────────────────────────────────────────
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Minimal identity + display info for a party (driver or passenger).
     * Avoids leaking heavy byte-array fields or auth internals into the wire.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartyDTO {
        private UUID    id;
        private String  fullName;
        private String  phoneNumber;
        private String  profilePictureUrl;
        private Double  avgRating;
        private Integer ratingCount;
    }
}
