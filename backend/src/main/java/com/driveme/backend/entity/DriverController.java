package com.driveme.backend.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for driver operations.
 */
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Slf4j
public class DriverController {

    private final DriverService driverService;
    private final DriverMapper driverMapper;

    /**
     * Sign up a new driver.
     * 
     * @param request the sign-up request containing driver details
     * @return the created driver
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody DriverSignUpRequest request) {
        try {
            log.info("Received sign-up request for email: {}", request.getEmail());
            Driver driver = driverService.signUp(request);
            
            // Convert to response DTO using mapper
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
