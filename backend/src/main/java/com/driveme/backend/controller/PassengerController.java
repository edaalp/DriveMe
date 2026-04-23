package com.driveme.backend.controller;

import com.driveme.backend.entity.Passenger;
import com.driveme.backend.helper.PassengerMapper;
import com.driveme.backend.dto.PassengerDTO;
import com.driveme.backend.service.PassengerService;
import com.driveme.backend.auth.PassengerSignUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

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
     * Accepts {@code multipart/form-data} with:
     * <ul>
     *   <li>{@code passenger} — JSON part containing sign-up fields</li>
     *   <li>{@code selfieFile} — optional selfie image (becomes profile picture)</li>
     *   <li>{@code tcPhotoFrontFile} — optional TC identity card front photo</li>
     *   <li>{@code tcPhotoBackFile}  — optional TC identity card back photo</li>
     * </ul>
     */
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signUp(
            @Valid @RequestPart("passenger") PassengerSignUpRequest request,
            @RequestPart(value = "selfieFile",       required = false) MultipartFile selfieFile,
            @RequestPart(value = "tcPhotoFrontFile", required = false) MultipartFile tcPhotoFrontFile,
            @RequestPart(value = "tcPhotoBackFile",  required = false) MultipartFile tcPhotoBackFile) {
        try {
            log.info("Received passenger sign-up request for email: {}", request.getEmail());
            Passenger passenger = passengerService.signUp(
                    request, selfieFile, tcPhotoFrontFile, tcPhotoBackFile);
            PassengerDTO response = passengerMapper.toDTO(passenger);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Sign-up failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during passenger sign-up", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred during sign-up"));
        }
    }

    /**
     * Get passenger by email.
     */
    @GetMapping
    public ResponseEntity<?> getByEmail(@RequestParam String email) {
        return passengerService.findByEmail(email)
                .map(passengerMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Download passenger profile picture (selfie) from database.
     */
    @GetMapping("/{id}/document/profile-picture")
    public ResponseEntity<byte[]> downloadProfilePicture(@PathVariable UUID id) {
        Passenger passenger = passengerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        if (passenger.getProfilePictureFile() == null || passenger.getProfilePictureFile().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(passenger.getProfilePictureFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + passenger.getProfilePictureFileName() + "\"")
                .body(passenger.getProfilePictureFile());
    }

    /**
     * Download passenger TC identity card front photo from database.
     */
    @GetMapping("/{id}/document/tc-front")
    public ResponseEntity<byte[]> downloadTcPhotoFront(@PathVariable UUID id) {
        Passenger passenger = passengerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        if (passenger.getTcPhotoFrontFile() == null || passenger.getTcPhotoFrontFile().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(passenger.getTcPhotoFrontFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + passenger.getTcPhotoFrontFileName() + "\"")
                .body(passenger.getTcPhotoFrontFile());
    }

    /**
     * Download passenger TC identity card back photo from database.
     */
    @GetMapping("/{id}/document/tc-back")
    public ResponseEntity<byte[]> downloadTcPhotoBack(@PathVariable UUID id) {
        Passenger passenger = passengerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        if (passenger.getTcPhotoBackFile() == null || passenger.getTcPhotoBackFile().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(passenger.getTcPhotoBackFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + passenger.getTcPhotoBackFileName() + "\"")
                .body(passenger.getTcPhotoBackFile());
    }

    private String guessContentType(String fileName) {
        if (fileName == null) return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private record ErrorResponse(String message) {}
}
