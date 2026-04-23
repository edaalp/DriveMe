package com.driveme.backend.controller;

import com.driveme.backend.auth.PassengerSignUpRequest;
import com.driveme.backend.dto.PassengerDTO;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.helper.PassengerMapper;
import com.driveme.backend.service.PassengerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for passenger operations.
 */
@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
@Slf4j
public class PassengerController {

    /** Same part name as driver signup selfie (Flutter must match). */
    public static final String SIGNUP_PART_SELFIE_FILE = "selfieFile";
    public static final String SIGNUP_PART_IDENTITY_FILE = "identityDocumentFile";

    private final PassengerService passengerService;
    private final PassengerMapper passengerMapper;

    /**
     * Sign up a new passenger: multipart JSON {@code passenger} + selfie + identity document (PDF/image).
     */
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signUp(
            @Valid @RequestPart("passenger") PassengerSignUpRequest request,
            @RequestPart(SIGNUP_PART_SELFIE_FILE) MultipartFile selfieFile,
            @RequestPart(SIGNUP_PART_IDENTITY_FILE) MultipartFile identityDocumentFile) {
        try {
            log.info(
                    "Passenger signup multipart email={} selfieBytes={} identityBytes={}",
                    request.getEmail(),
                    selfieFile.getSize(),
                    identityDocumentFile.getSize());
            Passenger passenger = passengerService.signUp(request, selfieFile, identityDocumentFile);
            
            // Convert to response DTO using mapper
            PassengerDTO response = passengerMapper.toDTO(passenger);
            
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
                .map(passengerMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Simple error response class.
     */
    private record ErrorResponse(String message) {}
}
