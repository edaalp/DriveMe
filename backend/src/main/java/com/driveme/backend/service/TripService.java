package com.driveme.backend.service;

import com.driveme.backend.common.Money;
import com.driveme.backend.common.PaymentMethod;
import com.driveme.backend.common.PaymentStatus;
import com.driveme.backend.common.PenaltyType;
import com.driveme.backend.common.TripStatus;
import com.driveme.backend.dto.CompleteTripRequest;
import com.driveme.backend.dto.DriverLocationPing;
import com.driveme.backend.dto.RateTripRequest;
import com.driveme.backend.dto.TripDTO;
import com.driveme.backend.entity.BaseUser;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.Penalty;
import com.driveme.backend.entity.Trip;
import com.driveme.backend.entity.TripRequest;
import com.driveme.backend.helper.TripMapper;
import com.driveme.backend.repository.DriverRepository;
import com.driveme.backend.repository.PassengerRepository;
import com.driveme.backend.repository.PenaltyRepository;
import com.driveme.backend.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates the {@code Trip} lifecycle: creation at acceptance time,
 * arrive / start / complete / cancel transitions, rating, and live driver
 * location updates.
 *
 * <p>All state transitions funnel through the domain helper methods on the
 * {@link Trip} entity itself, so invalid transitions (e.g. completing a
 * trip that was never started) always throw from one place.
 *
 * <p>Authorization is enforced per-endpoint by verifying that the caller
 * is either the driver or the passenger on the trip. Any other user gets
 * an {@link IllegalStateException} — translated to HTTP 403/400 by the
 * controller layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private static final BigDecimal CANCELLATION_PENALTY_AMOUNT = BigDecimal.valueOf(25);
    private static final double CANCELLATION_RATING_DROP = 0.25;
    private static final double NO_SHOW_RATING_DROP = 0.50;
    private static final double DEFAULT_ACCOUNT_RATING = 5.0;
    private static final double MIN_ACCOUNT_RATING = 1.0;
    private static final double MAX_ACCOUNT_RATING = 5.0;

    private final TripRepository          tripRepository;
    private final DriverRepository        driverRepository;
    private final PassengerRepository     passengerRepository;
    private final PenaltyRepository       penaltyRepository;
    private final TripMapper              tripMapper;

    // ── Creation ─────────────────────────────────────────────────────────────

    /**
     * Creates a new {@link Trip} from an accepted {@link TripRequest}.
     *
     * <p>Call this <strong>inside</strong> the same transaction that marks the
     * request as MATCHED so both rows are persisted atomically.
     *
     * @param request the accepted trip request (must be {@code MATCHED})
     * @param driver  the driver that accepted it (must match {@code request.matchedDriver})
     * @return the freshly-persisted Trip
     * @throws IllegalStateException if a Trip already exists for this request
     */
    @Transactional
    public Trip createFromAcceptedRequest(TripRequest request, Driver driver) {
        tripRepository.findByTripRequestId(request.getId()).ifPresent(existing -> {
            throw new IllegalStateException(
                    "Trip already exists for request " + request.getId() + " (id=" + existing.getId() + ")");
        });

        Trip trip = new Trip();
        trip.setTripRequestId(request.getId());
        trip.setDriver(driver);
        trip.setPassenger(request.getPassenger());
        trip.setVehicle(request.getVehicle());

        // Snapshot the route/preference/price so the Trip is self-contained.
        trip.setPickup(request.getPickup());
        trip.setDestination(request.getDestination());
        trip.setPlannedDistanceKm(request.getDistanceKm());
        trip.setPlannedDurationMinutes(estimateMinutes(request.getDistanceKm()));
        trip.setWithPet(request.isWithPet());

        // Lock in the agreed price. Fall back to passenger's max offer if no
        // explicit offerAmount was posted (older requests).
        if (request.getOfferAmount() != null) {
            trip.setAgreedAmount(new Money(
                    request.getOfferAmount().getAmount(),
                    request.getOfferAmount().getCurrency()));
        } else if (request.getMaxPrice() != null) {
            trip.setAgreedAmount(new Money(
                    request.getMaxPrice().getAmount(),
                    request.getMaxPrice().getCurrency()));
        }

        // Defaults — cash is the only payment method we currently support.
        trip.setPaymentMethod(PaymentMethod.CASH);
        trip.setPaymentStatus(PaymentStatus.PENDING);
        trip.setStatus(TripStatus.ACCEPTED);
        trip.setAcceptedAt(Instant.now());

        Trip saved = tripRepository.save(trip);
        log.info("Trip {} created from request {} (driver={}, passenger={})",
                saved.getId(), request.getId(),
                driver.getId(), request.getPassenger().getId());
        return saved;
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TripDTO getTripById(UUID tripId, UUID callerId) {
        Trip trip = mustLoadAndAuthorize(tripId, callerId);
        return tripMapper.toDTO(trip);
    }

    @Transactional(readOnly = true)
    public List<TripDTO> getTripsForDriver(UUID driverId) {
        return tripRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .stream()
                .map(tripMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TripDTO> getTripsForPassenger(UUID passengerId) {
        return tripRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId)
                .stream()
                .map(tripMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ── Lifecycle transitions ────────────────────────────────────────────────

    @Transactional
    public TripDTO markArrived(UUID tripId, UUID driverId) {
        Trip trip = mustLoadDriverTrip(tripId, driverId);
        trip.markDriverArrived();
        // Generate a fresh 4-digit boarding code each time the driver marks arrival.
        trip.setVerificationCode(String.format("%04d", 1000 + new Random().nextInt(9000)));
        return tripMapper.toDTO(tripRepository.save(trip));
    }

    /**
     * Issue a new 4-digit boarding code while the passenger is verifying.
     * Driver or passenger may call this during {@link TripStatus#DRIVER_ARRIVED}.
     */
    @Transactional
    public TripDTO refreshBoardingCode(UUID tripId, UUID callerId) {
        Trip trip = mustLoadAndAuthorize(tripId, callerId);
        if (trip.getStatus() != TripStatus.DRIVER_ARRIVED) {
            throw new IllegalStateException(
                    "Boarding code can only be refreshed when the driver has arrived at pickup (status DRIVER_ARRIVED).");
        }
        trip.setVerificationCode(String.format("%04d", 1000 + new Random().nextInt(9000)));
        Trip saved = tripRepository.save(trip);
        log.info("Trip {} boarding code refreshed by {}", saved.getId(), callerId);
        return tripMapper.toDTO(saved);
    }

    /**
     * Passenger confirms they are in the car by acknowledging the driver's code.
     * Transitions the trip from DRIVER_ARRIVED → IN_PROGRESS.
     *
     * @param tripId      the Trip entity id
     * @param passengerId the authenticated passenger
     */
    @Transactional
    public TripDTO confirmBoarding(UUID tripId, UUID passengerId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
        if (!trip.getPassenger().getId().equals(passengerId)) {
            throw new IllegalStateException("Only the trip's passenger may confirm boarding");
        }
        trip.start(); // DRIVER_ARRIVED → IN_PROGRESS
        return tripMapper.toDTO(tripRepository.save(trip));
    }

    @Transactional
    public TripDTO start(UUID tripId, UUID driverId) {
        Trip trip = mustLoadDriverTrip(tripId, driverId);
        trip.start();
        return tripMapper.toDTO(tripRepository.save(trip));
    }

    @Transactional
    public TripDTO complete(UUID tripId, UUID driverId, CompleteTripRequest body) {
        Trip trip = mustLoadDriverTrip(tripId, driverId);

        Double distance    = body != null ? body.getActualDistanceKm()      : null;
        Integer duration   = body != null ? body.getActualDurationMinutes() : null;
        String polyline    = body != null ? body.getRoutePolyline()         : null;

        trip.complete(distance, duration);
        if (polyline != null && !polyline.isBlank()) {
            trip.setRoutePolyline(polyline);
        }
        if (trip.getAgreedAmount() != null) {
            // Finalised amount defaults to the agreed amount unless an adjustment
            // is later applied (surge/wait fee would overwrite this).
            trip.setFinalAmount(new Money(
                    trip.getAgreedAmount().getAmount(),
                    trip.getAgreedAmount().getCurrency()));
        }

        return tripMapper.toDTO(tripRepository.save(trip));
    }

    @Transactional
    public TripDTO cancel(UUID tripId, UUID callerId, String reason) {
        Trip trip = mustLoadAndAuthorize(tripId, callerId);

        Trip.CancelledBy party = trip.getDriver().getId().equals(callerId)
                ? Trip.CancelledBy.DRIVER
                : Trip.CancelledBy.PASSENGER;
        boolean chargePenalty = shouldChargePenalty(party, trip);

        trip.cancelBy(party, reason);

        // Charge a flat fee to the side that caused a costly cancellation.
        if (chargePenalty) {
            trip.setPenaltyAmount(Money.ofTRY(CANCELLATION_PENALTY_AMOUNT));
            BaseUser penalizedUser = party == Trip.CancelledBy.DRIVER
                    ? trip.getDriver()
                    : trip.getPassenger();
            applyPenalty(
                    penalizedUser,
                    trip,
                    PenaltyType.CANCELLATION,
                    "Trip cancellation penalty",
                    CANCELLATION_RATING_DROP);
        }

        return tripMapper.toDTO(tripRepository.save(trip));
    }

    @Transactional
    public TripDTO markNoShow(UUID tripId, UUID driverId) {
        Trip trip = mustLoadDriverTrip(tripId, driverId);
        trip.markNoShow();
        // No-show still bills the passenger a nominal fee since the driver showed up.
        trip.setPenaltyAmount(Money.ofTRY(CANCELLATION_PENALTY_AMOUNT));
        applyPenalty(
                trip.getPassenger(),
                trip,
                PenaltyType.NO_SHOW,
                "Passenger did not show up at pickup",
                NO_SHOW_RATING_DROP);
        return tripMapper.toDTO(tripRepository.save(trip));
    }

    // ── Ratings ──────────────────────────────────────────────────────────────

    /**
     * Passenger rates the driver. Updates {@link BaseUser#getAvgRating()} of
     * the driver as a running average.
     */
    @Transactional
    public TripDTO ratePassengerToDriver(UUID tripId, UUID passengerId, RateTripRequest body) {
        Trip trip = mustLoadPassengerTrip(tripId, passengerId);
        requireCompleted(trip);
        if (trip.getRatingByPassenger() != null) {
            throw new IllegalStateException("You have already rated this trip");
        }

        trip.setRatingByPassenger(body.getRating());
        trip.setCommentByPassenger(trim(body.getComment()));
        applyRatingAggregate(trip.getDriver(), body.getRating());

        return tripMapper.toDTO(tripRepository.save(trip));
    }

    /**
     * Driver rates the passenger. Updates the passenger's running avg rating.
     */
    @Transactional
    public TripDTO rateDriverToPassenger(UUID tripId, UUID driverId, RateTripRequest body) {
        Trip trip = mustLoadDriverTrip(tripId, driverId);
        requireCompleted(trip);
        if (trip.getRatingByDriver() != null) {
            throw new IllegalStateException("You have already rated this trip");
        }

        trip.setRatingByDriver(body.getRating());
        trip.setCommentByDriver(trim(body.getComment()));
        applyRatingAggregate(trip.getPassenger(), body.getRating());

        return tripMapper.toDTO(tripRepository.save(trip));
    }

    // ── Live tracking ────────────────────────────────────────────────────────

    /**
     * Updates the driver's last-known location on the trip. Called frequently
     * by the driver's app while en route; the passenger's waiting screen polls
     * {@code GET /trips/{id}} to render a live marker.
     */
    @Transactional
    public TripDTO updateDriverLocation(UUID tripId, UUID driverId, DriverLocationPing ping) {
        Trip trip = mustLoadDriverTrip(tripId, driverId);
        if (trip.isFinished()) {
            // Stale pings after completion are ignored rather than erroring — the
            // driver's app may fire one last update before shutting down the worker.
            return tripMapper.toDTO(trip);
        }
        trip.setDriverLastLat(ping.getLat());
        trip.setDriverLastLon(ping.getLon());
        if (ping.getEtaToPickupMinutes() != null) {
            trip.setEtaToPickupMinutes(ping.getEtaToPickupMinutes());
        }
        return tripMapper.toDTO(tripRepository.save(trip));
    }

    // ── Internals ────────────────────────────────────────────────────────────

    /** Loads a trip and verifies the caller is a party to it (driver or passenger). */
    private Trip mustLoadAndAuthorize(UUID tripId, UUID callerId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        UUID driverId    = trip.getDriver()    != null ? trip.getDriver().getId()    : null;
        UUID passengerId = trip.getPassenger() != null ? trip.getPassenger().getId() : null;

        if (!callerId.equals(driverId) && !callerId.equals(passengerId)) {
            throw new IllegalStateException("You are not a participant of this trip");
        }
        return trip;
    }

    private Trip mustLoadDriverTrip(UUID tripId, UUID driverId) {
        Trip trip = mustLoadAndAuthorize(tripId, driverId);
        if (trip.getDriver() == null || !driverId.equals(trip.getDriver().getId())) {
            throw new IllegalStateException("Only the assigned driver can perform this action");
        }
        return trip;
    }

    private Trip mustLoadPassengerTrip(UUID tripId, UUID passengerId) {
        Trip trip = mustLoadAndAuthorize(tripId, passengerId);
        if (trip.getPassenger() == null || !passengerId.equals(trip.getPassenger().getId())) {
            throw new IllegalStateException("Only the owning passenger can perform this action");
        }
        return trip;
    }

    private static void requireCompleted(Trip trip) {
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Trip must be completed before rating (current status: " + trip.getStatus() + ")");
        }
    }

    /**
     * Applies an incoming rating to the user's running average:
     *
     * <p>{@code newAvg = (oldAvg * oldCount + rating) / (oldCount + 1)}
     *
     * <p>Persists through the same JPA session via the entity setters; the
     * caller's transaction takes care of flushing.
     */
    private void applyRatingAggregate(BaseUser user, int rating) {
        if (user == null) return;

        double oldAvg  = user.getAvgRating()   != null ? user.getAvgRating()   : DEFAULT_ACCOUNT_RATING;
        int   oldCount = user.getRatingCount() != null ? user.getRatingCount() : 0;

        int newCount = oldCount + 1;
        double newAvg = (oldAvg * oldCount + rating) / newCount;

        user.setAvgRating(newAvg);
        user.setRatingCount(newCount);

        if (user instanceof Driver d) {
            driverRepository.save(d);
        } else if (user instanceof Passenger p) {
            passengerRepository.save(p);
        }
    }

    private static boolean shouldChargePenalty(Trip.CancelledBy party, Trip trip) {
        // Driver cancelling: always charged.
        if (party == Trip.CancelledBy.DRIVER) return true;
        // Passenger cancelling: only after the driver has arrived.
        // Passenger cancelling immediately after acceptance is free.
        return trip.getStatus() == TripStatus.DRIVER_ARRIVED
                && trip.getDriverArrivedAt() != null;
    }

    private void applyPenalty(
            BaseUser user,
            Trip trip,
            PenaltyType type,
            String reason,
            double ratingDrop) {
        if (user == null || trip == null) {
            return;
        }

        Penalty penalty = new Penalty();
        penalty.setUserId(user.getId());
        penalty.setType(type);
        penalty.setPenaltyAmount(Money.ofTRY(CANCELLATION_PENALTY_AMOUNT));
        penalty.setReason(reason);
        penalty.setTripId(trip.getId());
        penalty.setPaid(false);
        penaltyRepository.save(penalty);

        double current = user.getAvgRating() != null ? user.getAvgRating() : DEFAULT_ACCOUNT_RATING;
        double next = Math.max(MIN_ACCOUNT_RATING, Math.min(MAX_ACCOUNT_RATING, current - ratingDrop));
        int oldCount = user.getRatingCount() != null ? user.getRatingCount() : 0;

        user.setAvgRating(next);
        user.setRatingCount(oldCount + 1);

        if (user instanceof Driver d) {
            driverRepository.save(d);
        } else if (user instanceof Passenger p) {
            passengerRepository.save(p);
        }
    }

    private static int estimateMinutes(double distanceKm) {
        // Simple heuristic: 60 km/h average city speed.
        return Math.max(1, (int) Math.round(distanceKm));
    }

    private static String trim(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }
}
