package com.driveme.backend.controller;

import com.driveme.backend.dto.CreateVehicleRequest;
import com.driveme.backend.dto.VehicleDTO;
import com.driveme.backend.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Vehicle operations.
 */
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * Get all vehicles for the authenticated passenger.
     * GET /api/vehicles/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<VehicleDTO>> getMyVehicles(Authentication authentication) {
        UUID passengerId = UUID.fromString(authentication.getPrincipal().toString());
        List<VehicleDTO> vehicles = vehicleService.getVehiclesByPassengerId(passengerId);
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Get a vehicle by ID.
     * GET /api/vehicles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<VehicleDTO> getVehicleById(@PathVariable UUID id) {
        VehicleDTO vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Create a new vehicle for the authenticated passenger.
     * POST /api/vehicles
     */
    @PostMapping
    public ResponseEntity<VehicleDTO> createVehicle(
            Authentication authentication,
            @Valid @RequestBody CreateVehicleRequest request) {
        UUID passengerId = UUID.fromString(authentication.getPrincipal().toString());
        VehicleDTO createdVehicle = vehicleService.createVehicle(passengerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVehicle);
    }

    /**
     * Upload a document for a vehicle (e.g. registration certificate).
     * POST /api/vehicles/{id}/document
     */
    @PostMapping("/{id}/document")
    public ResponseEntity<?> uploadDocument(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        try {
            UUID passengerId = UUID.fromString(authentication.getPrincipal().toString());
            VehicleDTO updated = vehicleService.uploadDocument(id, passengerId, file);
            return ResponseEntity.ok(updated);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "Failed to upload document"));
        }
    }

    /**
     * Delete a vehicle.
     * DELETE /api/vehicles/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID passengerId = UUID.fromString(authentication.getPrincipal().toString());
        vehicleService.deleteVehicle(id, passengerId);
        return ResponseEntity.noContent().build();
    }
}
