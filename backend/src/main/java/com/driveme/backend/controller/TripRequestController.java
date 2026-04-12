package com.driveme.backend.controller;

import com.driveme.backend.dto.CreateTripRequestRequest;
import com.driveme.backend.dto.TripRequestDTO;
import com.driveme.backend.service.TripRequestService;
import jakarta.validation.Valid;
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


}
