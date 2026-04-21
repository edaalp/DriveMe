package com.driveme.backend.controller;

import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.dto.AdminSignUpRequest;
import com.driveme.backend.dto.DriverResponse;
import com.driveme.backend.dto.VehicleDTO;
import com.driveme.backend.dto.VerificationDecisionRequest;
import com.driveme.backend.entity.Admin;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.entity.Vehicle;
import com.driveme.backend.helper.DriverMapper;
import com.driveme.backend.service.AdminService;
import com.driveme.backend.service.DriverService;
import com.driveme.backend.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for admin operations: signup, verification workflows, and document access.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin management and verification APIs")
public class AdminController {

    private final AdminService adminService;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final DriverMapper driverMapper;

    @Value("${admin.signup-secret}")
    private String signupSecret;

    // ==================== Admin Auth ====================

    @PostMapping("/signup")
    @Operation(summary = "Register admin", description = "Create a new admin account (requires X-Admin-Secret header)")
    public ResponseEntity<?> signUp(
            @RequestHeader("X-Admin-Secret") String secret,
            @Valid @RequestBody AdminSignUpRequest request) {
        try {
            if (!signupSecret.equals(secret)) {
                log.warn("Admin signup rejected: invalid secret");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Invalid admin secret"));
            }

            Admin admin = adminService.signUp(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", admin.getId().toString(),
                    "email", admin.getEmail(),
                    "fullName", admin.getFullName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ==================== Vehicle Verification ====================

    @GetMapping("/vehicles")
    @Operation(summary = "List vehicles", description = "List vehicles, optionally filtered by verification status")
    public ResponseEntity<List<VehicleDTO>> getVehicles(
            @RequestParam(required = false) VerificationStatus status) {
        List<VehicleDTO> vehicles = (status != null)
                ? vehicleService.getVehiclesByStatus(status)
                : vehicleService.getAllVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/vehicles/{id}")
    @Operation(summary = "Get vehicle detail", description = "Get full vehicle details for admin review")
    public ResponseEntity<VehicleDTO> getVehicleById(@PathVariable UUID id) {
        VehicleDTO vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/vehicles/{id}/verify")
    @Operation(summary = "Verify vehicle", description = "Approve or reject a vehicle")
    public ResponseEntity<?> verifyVehicle(
            @PathVariable UUID id,
            @Valid @RequestBody VerificationDecisionRequest request) {
        try {
            if (request.getDecision() == VerificationStatus.REJECTED
                    && (request.getReason() == null || request.getReason().isBlank())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Reason is required when rejecting"));
            }

            VehicleDTO updated = vehicleService.verifyVehicle(id, request.getDecision(), request.getReason());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/vehicles/{id}/document")
    @Operation(summary = "Download vehicle document", description = "Download the uploaded vehicle document")
    public ResponseEntity<byte[]> downloadVehicleDocument(@PathVariable UUID id) {
        Vehicle vehicle = vehicleService.getVehicleEntityById(id);

        if (vehicle.getDocumentFile() == null || vehicle.getDocumentFile().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(vehicle.getDocumentFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + vehicle.getDocumentFileName() + "\"")
                .body(vehicle.getDocumentFile());
    }

    // ==================== Driver Verification ====================

    @GetMapping("/drivers")
    @Operation(summary = "List drivers", description = "List drivers, optionally filtered by verification status")
    public ResponseEntity<List<DriverResponse>> getDrivers(
            @RequestParam(required = false) VerificationStatus status) {
        List<DriverResponse> drivers = (status != null)
                ? driverService.getDriversByVerificationStatus(status)
                : driverService.getAllDrivers();
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/drivers/{id}")
    @Operation(summary = "Get driver detail", description = "Get full driver details for admin review")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable UUID id) {
        Driver driver = driverService.getDriverEntityById(id);
        return ResponseEntity.ok(driverMapper.toResponse(driver));
    }

    @PutMapping("/drivers/{id}/verify")
    @Operation(summary = "Verify driver", description = "Approve or reject a driver")
    public ResponseEntity<?> verifyDriver(
            @PathVariable UUID id,
            @Valid @RequestBody VerificationDecisionRequest request) {
        try {
            if (request.getDecision() == VerificationStatus.REJECTED
                    && (request.getReason() == null || request.getReason().isBlank())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Reason is required when rejecting"));
            }

            DriverResponse updated = driverService.verifyDriver(id, request.getDecision(), request.getReason());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/drivers/{id}/document/criminal-record")
    @Operation(summary = "Download criminal record", description = "Download a driver's criminal record file")
    public ResponseEntity<byte[]> downloadCriminalRecord(@PathVariable UUID id) {
        Driver driver = driverService.getDriverEntityById(id);

        if (driver.getCriminalRecordFile() == null || driver.getCriminalRecordFile().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(driver.getCriminalRecordFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + driver.getCriminalRecordFileName() + "\"")
                .body(driver.getCriminalRecordFile());
    }

    @GetMapping("/drivers/{id}/document/license")
    @Operation(summary = "Download driver license", description = "Download a driver's license document from DB")
    public ResponseEntity<byte[]> downloadDriverLicense(@PathVariable UUID id) {
        Driver driver = driverService.getDriverEntityById(id);

        if (driver.getDriverLicenseFile() == null || driver.getDriverLicenseFile().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(driver.getDriverLicenseFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + driver.getDriverLicenseFileName() + "\"")
                .body(driver.getDriverLicenseFile());
    }

    @GetMapping("/drivers/{id}/document/profile-picture")
    @Operation(summary = "Download profile picture", description = "Download a driver's profile picture from DB")
    public ResponseEntity<byte[]> downloadProfilePicture(@PathVariable UUID id) {
        Driver driver = driverService.getDriverEntityById(id);

        if (driver.getProfilePictureFile() == null || driver.getProfilePictureFile().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(driver.getProfilePictureFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + driver.getProfilePictureFileName() + "\"")
                .body(driver.getProfilePictureFile());
    }

    // ==================== Dashboard Stats ====================

    @GetMapping("/stats")
    @Operation(summary = "Dashboard stats", description = "Get counts of pending vehicles and drivers")
    public ResponseEntity<Map<String, Object>> getStats() {
        long pendingVehicles = vehicleService.getVehiclesByStatus(VerificationStatus.PENDING).size();
        long pendingDrivers = driverService.getDriversByVerificationStatus(VerificationStatus.PENDING).size();

        return ResponseEntity.ok(Map.of(
                "pendingVehicles", pendingVehicles,
                "pendingDrivers", pendingDrivers
        ));
    }

    // ==================== Helpers ====================

    private String guessContentType(String fileName) {
        if (fileName == null) return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
