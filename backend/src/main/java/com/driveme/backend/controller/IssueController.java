package com.driveme.backend.controller;

import com.driveme.backend.dto.ReportIssueRequest;
import com.driveme.backend.dto.ReportedIssueDTO;
import com.driveme.backend.service.DriverService;
import com.driveme.backend.service.PassengerService;
import com.driveme.backend.service.ReportedIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for user-submitted issue reports.
 * Any authenticated user (passenger or driver) can submit a report.
 */
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Issues", description = "User-reported issue APIs")
public class IssueController {

    private final ReportedIssueService reportedIssueService;
    private final PassengerService passengerService;
    private final DriverService driverService;

    @PostMapping("/report")
    @Operation(summary = "Report an issue", description = "Submit an issue report from a passenger or driver")
    public ResponseEntity<?> reportIssue(
            @Valid @RequestBody ReportIssueRequest request,
            Authentication authentication) {
        try {
            UUID reporterId = UUID.fromString(authentication.getName());

            // Determine role from granted authority (ROLE_PASSENGER or ROLE_DRIVER)
            String role = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> a.equals("ROLE_PASSENGER") || a.equals("ROLE_DRIVER"))
                    .findFirst()
                    .orElse("ROLE_UNKNOWN")
                    .replace("ROLE_", "");

            // Resolve display name for the report record
            String fullName = resolveFullName(reporterId, role);

            ReportedIssueDTO saved = reportedIssueService.submit(reporterId, fullName, role, request);
            log.info("Issue reported by {} ({}): {}", fullName, role, saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error saving reported issue", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to submit issue. Please try again."));
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String resolveFullName(UUID userId, String role) {
        try {
            if ("PASSENGER".equals(role)) {
                return passengerService.findById(userId)
                        .map(p -> p.getFullName())
                        .orElse("Unknown Passenger");
            } else if ("DRIVER".equals(role)) {
                return driverService.getDriverEntityById(userId).getFullName();
            }
        } catch (Exception e) {
            log.warn("Could not resolve full name for {}: {}", userId, e.getMessage());
        }
        return "Unknown User";
    }
}
