package com.driveme.backend.service;

import com.driveme.backend.entity.Admin;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.repository.AdminRepository;
import com.driveme.backend.repository.DriverRepository;
import com.driveme.backend.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling the forgot-password / OTP reset flow.
 *
 * <p>OTP codes and reset tokens are stored in-memory with a 10-minute TTL.
 * In a production system these would be persisted to a cache (Redis) or DB
 * and the code would be delivered via email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** How long (ms) an OTP code stays valid before expiring. */
    private static final long OTP_TTL_MS = 10 * 60 * 1000L; // 10 minutes

    /** How long (ms) a reset token stays valid once the OTP is verified. */
    private static final long RESET_TOKEN_TTL_MS = 15 * 60 * 1000L; // 15 minutes

    // ── State ─────────────────────────────────────────────────────────────────

    /** email → pending OTP record */
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    /** resetToken → verified email record */
    private final Map<String, ResetTokenRecord> resetTokenStore = new ConcurrentHashMap<>();

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final DriverRepository    driverRepository;
    private final PassengerRepository passengerRepository;
    private final AdminRepository     adminRepository;
    private final PasswordEncoder     passwordEncoder;
    private final EmailService        emailService;
    private final SecureRandom        secureRandom = new SecureRandom();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Step 1 — generate a 6-digit OTP and store it for the given e-mail.
     *
     * <p>When {@code app.mail.enabled=true} the code is sent via SMTP and
     * {@code null} is returned (the controller must not expose it).
     * When email is disabled the code is logged and returned so the dev flow
     * works without an SMTP server.
     *
     * @param email the account e-mail
     * @return the OTP string when email is disabled, {@code null} when it was sent by email
     * @throws IllegalArgumentException if no active account exists for the email
     */
    public String generateOtp(String email) {
        String normalised = email.trim().toLowerCase();

        if (!accountExists(normalised)) {
            log.warn("Password-reset OTP requested for unknown email: {}", normalised);
            throw new IllegalArgumentException("No account found for this email address");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        otpStore.put(normalised, new OtpRecord(code, Instant.now().plusMillis(OTP_TTL_MS)));

        if (emailService.isEnabled()) {
            // Send the OTP via SMTP — do NOT return it to the caller.
            emailService.sendPasswordResetOtp(normalised, code);
            log.info("Password-reset OTP emailed to {}", normalised);
            return null; // caller must not expose the code in the response
        } else {
            // Dev mode: log it and return it so the API response can surface it.
            log.info("=== [DEV] PASSWORD RESET OTP for {} : {} (expires in 10 min) ===", normalised, code);
            return code;
        }
    }

    /**
     * Step 2 — verify the OTP and exchange it for a one-time reset token.
     *
     * @param email the account e-mail
     * @param code  the OTP the user submitted
     * @return a UUID reset token to be used in step 3
     * @throws IllegalArgumentException on invalid / expired code
     */
    public String verifyOtp(String email, String code) {
        String normalised = email.trim().toLowerCase();
        OtpRecord record = otpStore.get(normalised);

        if (record == null) {
            throw new IllegalArgumentException("No reset code was requested for this email");
        }
        if (Instant.now().isAfter(record.expiry())) {
            otpStore.remove(normalised);
            throw new IllegalArgumentException("Verification code has expired. Please request a new one");
        }
        if (!record.code().equals(code.trim())) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        // Code is valid — invalidate it and issue a reset token
        otpStore.remove(normalised);
        String resetToken = UUID.randomUUID().toString();
        resetTokenStore.put(resetToken, new ResetTokenRecord(normalised, Instant.now().plusMillis(RESET_TOKEN_TTL_MS)));

        log.info("OTP verified for {}. Reset token issued.", normalised);
        return resetToken;
    }

    /**
     * Step 3 — consume the reset token and update the user's password.
     *
     * @param resetToken  the token obtained from step 2
     * @param newPassword the desired new password (plain text; will be hashed)
     * @throws IllegalArgumentException on invalid / expired token
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        ResetTokenRecord record = resetTokenStore.get(resetToken);

        if (record == null) {
            throw new IllegalArgumentException("Invalid or already-used reset token");
        }
        if (Instant.now().isAfter(record.expiry())) {
            resetTokenStore.remove(resetToken);
            throw new IllegalArgumentException("Reset token has expired. Please start over");
        }

        String email   = record.email();
        String hashed  = passwordEncoder.encode(newPassword);
        boolean updated = false;

        // Try Driver
        Optional<Driver> driverOpt = driverRepository.findByEmail(email);
        if (driverOpt.isPresent()) {
            Driver driver = driverOpt.get();
            driver.setPasswordHash(hashed);
            driverRepository.save(driver);
            updated = true;
            log.info("Password reset successful for driver: {}", email);
        }

        // Try Passenger
        if (!updated) {
            Optional<Passenger> passengerOpt = passengerRepository.findByEmail(email);
            if (passengerOpt.isPresent()) {
                Passenger passenger = passengerOpt.get();
                passenger.setPasswordHash(hashed);
                passengerRepository.save(passenger);
                updated = true;
                log.info("Password reset successful for passenger: {}", email);
            }
        }

        // Try Admin
        if (!updated) {
            Optional<Admin> adminOpt = adminRepository.findByEmail(email);
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                admin.setPasswordHash(hashed);
                adminRepository.save(admin);
                updated = true;
                log.info("Password reset successful for admin: {}", email);
            }
        }

        if (!updated) {
            throw new IllegalStateException("Account no longer exists for email: " + email);
        }

        // Invalidate the reset token so it can't be reused
        resetTokenStore.remove(resetToken);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean accountExists(String email) {
        return driverRepository.findByEmail(email).isPresent()
            || passengerRepository.findByEmail(email).isPresent()
            || adminRepository.findByEmail(email).isPresent();
    }

    // ── Records ───────────────────────────────────────────────────────────────

    private record OtpRecord(String code, Instant expiry) {}

    private record ResetTokenRecord(String email, Instant expiry) {}
}
