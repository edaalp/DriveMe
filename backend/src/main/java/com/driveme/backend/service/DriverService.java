package com.driveme.backend.service;

import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.dto.DriverResponse;
import com.driveme.backend.dto.DriverSignUpRequest;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.helper.DriverMapper;
import com.driveme.backend.repository.DriverRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing driver operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Register a new driver with uploaded documents.
     *
     * @param request the sign-up request (JSON part)
     * @param licenseFile driver's license scan
     * @param criminalRecordFile criminal record certificate
     * @return the created driver
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public Driver signUp(
            @Valid DriverSignUpRequest request,
            MultipartFile licenseFile,
            MultipartFile criminalRecordFile,
            MultipartFile selfieFile) {
        log.info("Attempting to sign up driver with email: {}", request.getEmail());

        if (licenseFile == null || licenseFile.isEmpty()) {
            throw new IllegalArgumentException("License file is required");
        }
        if (criminalRecordFile == null || criminalRecordFile.isEmpty()) {
            throw new IllegalArgumentException("Criminal record file is required");
        }
        if (selfieFile == null || selfieFile.isEmpty()) {
            throw new IllegalArgumentException("Selfie photo is required");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("Sign up failed: Passwords do not match");
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (driverRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Sign up failed: Email already exists - {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        long tckNo = request.getTckNo();
        if (String.valueOf(tckNo).length() != 11) {
            log.warn("Sign up failed: Invalid TCK number");
            throw new IllegalArgumentException("TCK number must be 11 digits");
        }

        String licenseUrl = storeDriverDocument(licenseFile);
        String criminalUrl = storeDriverDocument(criminalRecordFile);
        String profilePicturePath = storeProfilePicture(selfieFile);

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        Driver driver = driverMapper.toEntity(request, hashedPassword, licenseUrl, criminalUrl, profilePicturePath);

        Driver savedDriver = driverRepository.save(driver);
        log.info("Driver successfully signed up with ID: {}", savedDriver.getId());

        return savedDriver;
    }

    private String storeDriverDocument(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            original = "document";
        }
        String safeName = Paths.get(original).getFileName().toString();
        if (safeName.contains("..")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String storedName = buildSanitizedStoredFilename(safeName);

        Path dir = Paths.get(uploadDir, "drivers").toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to store driver document", e);
            throw new IllegalArgumentException("Could not store uploaded file");
        }

        return "/uploads/drivers/" + storedName;
    }

    /**
     * Stores the profile (selfie) image under the configured upload root (e.g. {@code static/uploads/})
     * so it is served at {@code /uploads/&lt;filename&gt;}.
     */
    private String storeProfilePicture(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            original = inferSelfieFilenameFromContentType(file.getContentType());
        }
        String safeName = Paths.get(original).getFileName().toString();
        if (safeName.contains("..")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String lower = safeName.toLowerCase(Locale.ROOT);
        if (!hasAllowedProfileExtension(lower)) {
            String inferred = inferSelfieFilenameFromContentType(file.getContentType());
            safeName = Paths.get(inferred).getFileName().toString();
            lower = safeName.toLowerCase(Locale.ROOT);
        }
        if (!hasAllowedProfileExtension(lower)) {
            throw new IllegalArgumentException("Profile picture must be JPG, JPEG, or PNG");
        }

        String storedName = buildSanitizedStoredFilename(safeName);

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to store profile picture", e);
            throw new IllegalArgumentException("Could not store profile picture");
        }

        return "/uploads/" + storedName;
    }

    private static boolean hasAllowedProfileExtension(String lowerFilename) {
        return lowerFilename.endsWith(".jpg")
                || lowerFilename.endsWith(".jpeg")
                || lowerFilename.endsWith(".png");
    }

    /**
     * Fallback when {@code filename} is missing or has no extension; maps Content-Type to a dummy name
     * so {@link #buildSanitizedStoredFilename} can derive the stored extension (jpg / jpeg / png).
     */
    private static String inferSelfieFilenameFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "selfie.jpg";
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("png")) {
            return "selfie.png";
        }
        if (ct.contains("jpeg")) {
            return "selfie.jpeg";
        }
        if (ct.contains("jpg")) {
            return "selfie.jpg";
        }
        return "selfie.jpg";
    }

    /**
     * Produces a URL-safe stored name: sanitized original stem + UUID + ASCII extension
     * (spaces to underscores, Turkish letters transliterated).
     */
    private static String buildSanitizedStoredFilename(String safeName) {
        int dot = safeName.lastIndexOf('.');
        String base = dot > 0 ? safeName.substring(0, dot) : safeName;
        String rawExt = (dot >= 0 && dot < safeName.length() - 1)
                ? safeName.substring(dot + 1)
                : "";

        String cleanStem = sanitizeFilenamePart(base, 80);
        if (cleanStem.isEmpty()) {
            cleanStem = "document";
        }
        String cleanExt = sanitizeExtension(rawExt);
        return cleanStem + "_" + UUID.randomUUID() + "." + cleanExt;
    }

    private static String sanitizeExtension(String rawExt) {
        String part = sanitizeFilenamePart(rawExt, 12);
        if (part.isEmpty()) {
            return "bin";
        }
        return part;
    }

    /**
     * Keeps only [a-z0-9_] after transliteration; spaces become underscores.
     */
    private static String sanitizeFilenamePart(String input, int maxLen) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String t = input.trim().replace(' ', '_');
        t = transliterateTurkishToAscii(t);
        t = t.toLowerCase(Locale.ROOT);
        t = t.replaceAll("[^a-z0-9_]", "");
        if (t.length() > maxLen) {
            t = t.substring(0, maxLen);
        }
        return t;
    }

    private static String transliterateTurkishToAscii(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(switch (c) {
                case 'ç', 'Ç' -> 'c';
                case 'ğ', 'Ğ' -> 'g';
                case 'ı', 'İ' -> 'i';
                case 'ö', 'Ö' -> 'o';
                case 'ş', 'Ş' -> 's';
                case 'ü', 'Ü' -> 'u';
                default -> c;
            });
        }
        return sb.toString();
    }

    /**
     * Find driver by email.
     *
     * @param email the email to search for
     * @return optional containing the driver if found
     */
    @Transactional(readOnly = true)
    public Optional<Driver> findByEmail(String email) {
        return driverRepository.findByEmail(email);
    }

    /**
     * Update driver availability status.
     *
     * @param driverId the driver ID
     * @param available the new availability status
     * @return the updated driver
     * @throws IllegalArgumentException if driver not found
     */
    @Transactional
    public Driver updateAvailability(String driverId, boolean available) {
        log.info("Updating availability for driver: {}", driverId);

        Driver driver = driverRepository.findById(java.util.UUID.fromString(driverId))
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        driver.setAvailable(available);
        Driver updatedDriver = driverRepository.save(driver);

        log.info("Driver {} availability updated to: {}", driverId, available);
        return updatedDriver;
    }

    // ---- Admin verification methods ----

    @Transactional(readOnly = true)
    public List<DriverResponse> getDriversByVerificationStatus(VerificationStatus status) {
        return driverRepository.findByVerificationStatus(status)
                .stream()
                .map(driverMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findAll()
                .stream()
                .map(driverMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DriverResponse verifyDriver(UUID driverId, VerificationStatus decision, String reason) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + driverId));

        if (decision == VerificationStatus.PENDING) {
            throw new IllegalArgumentException("Decision must be VERIFIED or REJECTED");
        }

        driver.setVerificationStatus(decision);
        driver.setRejectionReason(decision == VerificationStatus.REJECTED ? reason : null);

        Driver saved = driverRepository.save(driver);
        log.info("Driver {} verification updated to: {}", driverId, decision);
        return driverMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Driver getDriverEntityById(UUID driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + driverId));
    }
}
