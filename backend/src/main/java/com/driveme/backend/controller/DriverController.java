package com.driveme.backend.controller;

import com.driveme.backend.dto.DriverMePatchRequest;
import com.driveme.backend.dto.DriverResponse;
import com.driveme.backend.dto.DriverSignUpRequest;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.helper.DriverMapper;
import com.driveme.backend.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for driver operations.
 */
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Slf4j
public class DriverController {

    /** Multipart part name for selfie; must match Flutter {@code kDriverSignupSelfiePartName}. */
    public static final String SIGNUP_PART_SELFIE_FILE = "selfieFile";

    private final DriverService driverService;
    private final DriverMapper driverMapper;

    /**
     * Sign up a new driver (multipart: JSON {@code driver} + license and criminal record files).
     *
     * @param request driver fields as JSON
     * @param licenseFile license document
     * @param criminalRecordFile criminal record document
     * @param selfieFile profile photo (selfie), stored and exposed under {@code /uploads/}
     * @return the created driver
     */
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signUp(
            @Valid @RequestPart("driver") DriverSignUpRequest request,
            @RequestPart("licenseFile") MultipartFile licenseFile,
            @RequestPart("criminalRecordFile") MultipartFile criminalRecordFile,
            @RequestPart(SIGNUP_PART_SELFIE_FILE) MultipartFile selfieFile) {
        try {
            log.info("Received multipart sign-up request for email: {}", request.getEmail());
            Driver driver = driverService.signUp(request, licenseFile, criminalRecordFile, selfieFile);

            DriverResponse response = driverMapper.toResponse(driver);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Sign-up failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during sign-up", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred during sign-up"));
        }
    }

    /**
     * Current authenticated driver's profile (JWT subject is driver id).
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentDriver(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean isDriver = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DRIVER"::equals);
        if (!isDriver) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only drivers can access this resource"));
        }
        try {
            UUID id = UUID.fromString(authentication.getPrincipal().toString());
            Driver driver = driverService.getDriverEntityById(id);
            return ResponseEntity.ok(driverMapper.toResponse(driver));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Partial update for the current driver (e.g. {@code acceptsPets}).
     */
    @PatchMapping("/me")
    public ResponseEntity<?> patchCurrentDriver(
            Authentication authentication,
            @RequestBody DriverMePatchRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean isDriver = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DRIVER"::equals);
        if (!isDriver) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only drivers can access this resource"));
        }
        if (request.getAcceptsPets() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("No updatable fields provided"));
        }
        try {
            UUID id = UUID.fromString(authentication.getPrincipal().toString());
            DriverResponse updated = driverService.patchCurrentDriver(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get driver by email.
     *
     * @param email the email to search for
     * @return the driver if found
     */
    @GetMapping
    public ResponseEntity<?> getByEmail(@RequestParam String email) {
        return driverService.findByEmail(email)
                .map(driverMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update driver availability status.
     *
     * @param driverId the driver ID
     * @param request the availability update request
     * @return the updated driver
     */
    @PatchMapping("/{driverId}/availability")
    public ResponseEntity<?> updateAvailability(
            @PathVariable String driverId,
            @RequestBody AvailabilityRequest request) {
        try {
            Driver driver = driverService.updateAvailability(driverId, request.available());
            DriverResponse response = driverMapper.toResponse(driver);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update availability: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating availability", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while updating availability"));
        }
    }

    /**
     * Simple error response class.
     */
    private record ErrorResponse(String message) {}

    /**
     * Request body for updating driver availability.
     */
    private record AvailabilityRequest(boolean available) {}
}
