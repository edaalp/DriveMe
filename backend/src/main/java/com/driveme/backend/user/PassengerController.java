package com.driveme.backend.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for passenger operations.
 */
@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
@Slf4j
public class PassengerController {

    private final PassengerService passengerService;
    private final PassengerMapper passengerMapper;

    /**
     * Sign up a new passenger.
     * 
     * @param request the sign-up request containing passenger details
     * @return the created passenger
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody PassengerSignUpRequest request) {
        try {
            log.info("Received sign-up request for email: {}", request.getEmail());
            Passenger passenger = passengerService.signUp(request);
            
            // Convert to response DTO using mapper
            PassengerResponse response = passengerMapper.toResponse(passenger);
            
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
     * Get passenger by email.
     * 
     * @param email the email to search for
     * @return the passenger if found
     */
    @GetMapping
    public ResponseEntity<?> getByEmail(@RequestParam String email) {
        return passengerService.findByEmail(email)
                .map(passengerMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Simple error response class.
     */
    private record ErrorResponse(String message) {}
}
