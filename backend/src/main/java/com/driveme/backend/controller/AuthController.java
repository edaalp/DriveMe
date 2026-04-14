package com.driveme.backend.controller;

import com.driveme.backend.dto.ForgotPasswordRequest;
import com.driveme.backend.dto.LoginRequest;
import com.driveme.backend.dto.LoginResponse;
import com.driveme.backend.dto.ResetPasswordRequest;
import com.driveme.backend.dto.VerifyResetCodeRequest;
import com.driveme.backend.service.AuthService;
import com.driveme.backend.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for authentication operations.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /**
     * Login endpoint for both drivers and passengers.
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and receive JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials or inactive account"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login request received for email: {}", request.getEmail());
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }

    // ── Forgot-password flow ──────────────────────────────────────────────────

    /**
     * Step 1 — request a password-reset OTP for the given e-mail.
     *
     * <p>The OTP is currently printed to the server log (no SMTP configured).
     * For the demo build it is also returned in the response body under
     * {@code "code"} so the tester can enter it without checking logs.
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            // generateOtp returns the code when email is disabled (dev mode),
            // or null when the code was delivered via SMTP (prod mode).
            String devCode = passwordResetService.generateOtp(request.getEmail());
            log.info("Forgot-password OTP generated for: {}", request.getEmail());

            if (devCode != null) {
                // Dev mode — return code in response body so testers can use it
                // without checking server logs. Remove "code" before going to production.
                return ResponseEntity.ok(Map.of(
                    "message", "A verification code has been sent. (Dev mode: code included below)",
                    "code", devCode
                ));
            } 

            // Email was sent — do NOT include the code in the response
            return ResponseEntity.ok(Map.of(
                "message", "A verification code has been sent to your email address"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Step 2 — verify the OTP and receive a one-time reset token.
     */
    @PostMapping("/verify-reset-code")
    @Operation(summary = "Verify password reset OTP")
    public ResponseEntity<?> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        try {
            String resetToken = passwordResetService.verifyOtp(request.getEmail(), request.getCode());
            return ResponseEntity.ok(Map.of(
                "message", "Code verified successfully",
                "resetToken", resetToken
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error verifying reset code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Step 3 — consume the reset token and set a new password.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using verified token")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getResetToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error resetting password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Error response DTO for consistent error handling.
     */
    private record ErrorResponse(String message) {}
}
