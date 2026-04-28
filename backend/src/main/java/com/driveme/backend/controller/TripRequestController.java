package com.driveme.backend.controller;

import com.driveme.backend.dto.CreateTripRequestRequest;
import com.driveme.backend.dto.TripRequestDTO;
import com.driveme.backend.dto.*;
import com.driveme.backend.service.TripRequestService;
import com.driveme.backend.service.TripMatchingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for TripRequest operations.
 */
@RestController
@RequestMapping("/api/trip-requests")
@RequiredArgsConstructor
public class TripRequestController {

    private final TripRequestService tripRequestService;
    private final TripMatchingService tripMatchingService;

    /**
     * Create a new trip request.
     * POST /api/trip-requests
     */
    @PostMapping
    public ResponseEntity<TripRequestDTO> createTripRequest(
            Authentication authentication,
            @Valid @RequestBody CreateTripRequestRequest request) {
        UUID passengerId = UUID.fromString(authentication.getPrincipal().toString());
        TripRequestDTO createdRequest = tripRequestService.createTripRequest(passengerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
    }

    /**
     * Get a trip request by ID.
     * GET /api/trip-requests/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripRequestDTO> getTripRequestById(@PathVariable UUID id) {
        TripRequestDTO tripRequest = tripRequestService.getTripRequestById(id);
        return ResponseEntity.ok(tripRequest);
    }

    /**
     * Get all trip requests for the authenticated passenger.
     * GET /api/trip-requests/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<TripRequestDTO>> getMyTripRequests(Authentication authentication) {
        UUID passengerId = UUID.fromString(authentication.getPrincipal().toString());
        List<TripRequestDTO> tripRequests = tripRequestService.getTripRequestsByPassengerId(passengerId);
        return ResponseEntity.ok(tripRequests);
    }

    /**
     * Get all pending trip requests (for drivers).
     * GET /api/trip-requests/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<TripRequestDTO>> getPendingTripRequests() {
        List<TripRequestDTO> tripRequests = tripRequestService.getPendingTripRequests();
        return ResponseEntity.ok(tripRequests);
    }

    /**
     * Cancel a trip request.
     * PUT /api/trip-requests/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<TripRequestDTO> cancelTripRequest(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID passengerId = UUID.fromString(authentication.getPrincipal().toString());
        TripRequestDTO cancelledRequest = tripRequestService.cancelTripRequest(id, passengerId);
        return ResponseEntity.ok(cancelledRequest);
    }


    /**
     * Get matching status for a trip request (passenger view).
     * GET /api/trip-requests/{tripId}/matching-status
     */
    @GetMapping("/{tripId}/matching-status")
    public ResponseEntity<MatchingStatusDTO> getMatchingStatus(@PathVariable UUID tripId) {
        MatchingStatusDTO status = tripMatchingService.getMatchingStatus(tripId);
        return ResponseEntity.ok(status);
    }

    /**
     * Get available trip requests for the authenticated driver.
     * GET /api/trip-requests/driver/available?driverLat=41.0082&driverLon=28.9784&pickupRadiusKm=10&tripRadiusKm=50
     */
    @GetMapping("/driver/available")
    public ResponseEntity<List<AvailableTripRequestDTO>> getAvailableTrips(
            Authentication authentication,
            @RequestParam double driverLat,
            @RequestParam double driverLon,
            @RequestParam double pickupRadiusKm,
            @RequestParam double tripRadiusKm) {
        UUID driverId = UUID.fromString(authentication.getPrincipal().toString());
        List<AvailableTripRequestDTO> availableTrips = tripMatchingService.getAvailableTripsForDriver(
                driverId, driverLat, driverLon, pickupRadiusKm, tripRadiusKm);
        return ResponseEntity.ok(availableTrips);
    }

    /**
     * Accept a trip request as a driver.
     * POST /api/trip-requests/{tripId}/accept
     */
    @PostMapping("/{tripId}/accept")
    public ResponseEntity<TripAcceptanceResponseDTO> acceptTrip(
            Authentication authentication,
            @PathVariable UUID tripId) {
        UUID driverId = UUID.fromString(authentication.getPrincipal().toString());
        TripAcceptanceResponseDTO response = tripMatchingService.acceptTrip(tripId, driverId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Reject a trip request as a driver.
     * POST /api/trip-requests/{tripId}/reject
     */
    @PostMapping("/{tripId}/reject")
    public ResponseEntity<Void> rejectTrip(
            Authentication authentication,
            @PathVariable UUID tripId) {
        UUID driverId = UUID.fromString(authentication.getPrincipal().toString());
        tripMatchingService.rejectTrip(tripId, driverId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete/Cancel a trip request (passenger initiated).
     * DELETE /api/trip-requests/{tripId}
     */
    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(
            Authentication authentication,
            @PathVariable UUID tripId) {
        UUID passengerId = UUID.fromString(authentication.getPrincipal().toString());
        tripMatchingService.cancelTrip(tripId, passengerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Inner class for calculate price request body.
     */
    @lombok.Data
    public static class CalculatePriceRequest {
        @NotNull(message = "Pickup location is required")
        @Valid
        private LocationDTO pickup;

        @NotNull(message = "Destination location is required")
        @Valid
        private LocationDTO destination;
    }
}
