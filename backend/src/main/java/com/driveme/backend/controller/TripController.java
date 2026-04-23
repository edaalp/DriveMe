package com.driveme.backend.controller;

import com.driveme.backend.dto.CancelTripRequest;
import com.driveme.backend.dto.CompleteTripRequest;
import com.driveme.backend.dto.DriverLocationPing;
import com.driveme.backend.dto.RateTripRequest;
import com.driveme.backend.dto.TripDTO;
import com.driveme.backend.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for confirmed {@code Trip}s.
 *
 * <p>Endpoints are organised as follows:
 * <ul>
 *   <li>{@code GET /api/trips/{id}} — fetch a single trip (both parties allowed)</li>
 *   <li>{@code GET /api/trips/driver/me} — driver's trip history</li>
 *   <li>{@code GET /api/trips/passenger/me} — passenger's trip history</li>
 *   <li>{@code PUT /api/trips/{id}/arrived} — driver at pickup</li>
 *   <li>{@code PUT /api/trips/{id}/start}   — ride underway</li>
 *   <li>{@code PUT /api/trips/{id}/complete} — ride finished</li>
 *   <li>{@code PUT /api/trips/{id}/cancel}   — cancel (either party)</li>
 *   <li>{@code PUT /api/trips/{id}/no-show}  — driver marks passenger no-show</li>
 *   <li>{@code PUT /api/trips/{id}/location} — live driver location ping</li>
 *   <li>{@code POST /api/trips/{id}/rate/passenger-to-driver}</li>
 *   <li>{@code POST /api/trips/{id}/rate/driver-to-passenger}</li>
 * </ul>
 *
 * <p>Authorisation is enforced inside {@link TripService} — the controller
 * just extracts the caller's id from the JWT principal and forwards it.
 */
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    // ── Read ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<TripDTO> getTripById(Authentication auth, @PathVariable UUID id) {
        UUID callerId = callerId(auth);
        return ResponseEntity.ok(tripService.getTripById(id, callerId));
    }

    @GetMapping("/driver/me")
    public ResponseEntity<List<TripDTO>> myDriverTrips(Authentication auth) {
        return ResponseEntity.ok(tripService.getTripsForDriver(callerId(auth)));
    }

    @GetMapping("/passenger/me")
    public ResponseEntity<List<TripDTO>> myPassengerTrips(Authentication auth) {
        return ResponseEntity.ok(tripService.getTripsForPassenger(callerId(auth)));
    }

    // ── Lifecycle transitions (driver) ───────────────────────────────────────

    @PutMapping("/{id}/arrived")
    public ResponseEntity<TripDTO> driverArrived(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.ok(tripService.markArrived(id, callerId(auth)));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<TripDTO> startTrip(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.ok(tripService.start(id, callerId(auth)));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<TripDTO> completeTrip(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid CompleteTripRequest body) {
        return ResponseEntity.ok(tripService.complete(id, callerId(auth), body));
    }

    @PutMapping("/{id}/no-show")
    public ResponseEntity<TripDTO> noShow(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.ok(tripService.markNoShow(id, callerId(auth)));
    }

    /**
     * Passenger confirms they have boarded the vehicle (after seeing the driver's
     * 4-digit code). Transitions the trip DRIVER_ARRIVED → IN_PROGRESS so both
     * sides advance automatically.
     */
    @PostMapping("/{id}/confirm-boarding")
    public ResponseEntity<TripDTO> confirmBoarding(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.ok(tripService.confirmBoarding(id, callerId(auth)));
    }

    // ── Cancellation (either party) ──────────────────────────────────────────

    @PutMapping("/{id}/cancel")
    public ResponseEntity<TripDTO> cancelTrip(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid CancelTripRequest body) {
        String reason = body != null ? body.getReason() : null;
        return ResponseEntity.ok(tripService.cancel(id, callerId(auth), reason));
    }

    // ── Live tracking ────────────────────────────────────────────────────────

    @PutMapping("/{id}/location")
    public ResponseEntity<TripDTO> pingLocation(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody DriverLocationPing body) {
        return ResponseEntity.ok(tripService.updateDriverLocation(id, callerId(auth), body));
    }

    // ── Ratings ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/rate/passenger-to-driver")
    public ResponseEntity<TripDTO> ratePassengerToDriver(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody RateTripRequest body) {
        return ResponseEntity.ok(tripService.ratePassengerToDriver(id, callerId(auth), body));
    }

    @PostMapping("/{id}/rate/driver-to-passenger")
    public ResponseEntity<TripDTO> rateDriverToPassenger(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody RateTripRequest body) {
        return ResponseEntity.ok(tripService.rateDriverToPassenger(id, callerId(auth), body));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Extracts the caller's UUID from the JWT-populated {@link Authentication} principal. */
    private static UUID callerId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        return UUID.fromString(auth.getPrincipal().toString());
    }
}
