package com.driveme.backend.service;

import com.driveme.backend.entity.Passenger;
import com.driveme.backend.helper.PassengerMapper;
import com.driveme.backend.repository.PassengerRepository;
import com.driveme.backend.auth.PassengerSignUpRequest;
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

/**
 * Service for managing passenger operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final PassengerMapper passengerMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Register a new passenger (no documents).
     */
    @Transactional
    public Passenger signUp(PassengerSignUpRequest request) {
        return signUp(request, null, null, null);
    }

    /**
     * Register a new passenger with optional document photos.
     *
     * @param request          sign-up fields
     * @param selfieFile       selfie — becomes the profile picture (nullable)
     * @param tcPhotoFrontFile front of the TC identity card (nullable)
     * @param tcPhotoBackFile  back of the TC identity card (nullable)
     * @return the persisted Passenger
     */
    @Transactional
    public Passenger signUp(
            PassengerSignUpRequest request,
            MultipartFile selfieFile,
            MultipartFile tcPhotoFrontFile,
            MultipartFile tcPhotoBackFile) {

        log.info("Attempting to sign up passenger with email: {}", request.getEmail());

        if (passengerRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Sign up failed: Email already exists - {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        String profilePictureUrl = null;
        if (selfieFile != null && !selfieFile.isEmpty()) {
            profilePictureUrl = storeFile(selfieFile, true);
            log.info("Stored passenger selfie at: {}", profilePictureUrl);
        }

        String tcPhotoFrontUrl = null;
        if (tcPhotoFrontFile != null && !tcPhotoFrontFile.isEmpty()) {
            tcPhotoFrontUrl = storeFile(tcPhotoFrontFile, true);
            log.info("Stored TC front photo at: {}", tcPhotoFrontUrl);
        }

        String tcPhotoBackUrl = null;
        if (tcPhotoBackFile != null && !tcPhotoBackFile.isEmpty()) {
            tcPhotoBackUrl = storeFile(tcPhotoBackFile, true);
            log.info("Stored TC back photo at: {}", tcPhotoBackUrl);
        }

        Passenger passenger = passengerMapper.toEntity(
                request, hashedPassword, profilePictureUrl, tcPhotoFrontUrl, tcPhotoBackUrl);
        Passenger saved = passengerRepository.save(passenger);
        log.info("Passenger successfully signed up with ID: {}", saved.getId());
        return saved;
    }

    /** Find passenger by email. */
    public Optional<Passenger> findByEmail(String email) {
        return passengerRepository.findByEmail(email);
    }

    /** Return all passengers (for admin listing). */
    public List<Passenger> findAll() {
        return passengerRepository.findAll();
    }

    /** Find passenger by UUID. */
    public Optional<Passenger> findById(java.util.UUID id) {
        return passengerRepository.findById(id);
    }

    // ── File storage helpers ──────────────────────────────────────────────────

    /**
     * Stores an image/document file under the configured upload directory and
     * returns its public URL path (e.g. {@code /uploads/selfie_uuid.jpg}).
     *
     * @param file          the uploaded file
     * @param requireImage  when true only JPG/PNG are accepted; when false PDFs are also allowed
     */
    private String storeFile(MultipartFile file, boolean requireImage) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            original = inferFilenameFromContentType(file.getContentType(), requireImage);
        }
        String safeName = Paths.get(original).getFileName().toString();
        if (safeName.contains("..")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String lower = safeName.toLowerCase(Locale.ROOT);
        if (!isAllowedExtension(lower, requireImage)) {
            String inferred = inferFilenameFromContentType(file.getContentType(), requireImage);
            safeName = Paths.get(inferred).getFileName().toString();
            lower = safeName.toLowerCase(Locale.ROOT);
        }
        if (!isAllowedExtension(lower, requireImage)) {
            throw new IllegalArgumentException(requireImage
                    ? "File must be JPG, JPEG, or PNG"
                    : "File must be JPG, JPEG, PNG, or PDF");
        }

        String storedName = buildSanitizedStoredFilename(safeName);
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new IllegalArgumentException("Could not store uploaded file");
        }
        return "/uploads/" + storedName;
    }

    private static boolean isAllowedExtension(String lower, boolean requireImage) {
        boolean isImage = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
        return requireImage ? isImage : (isImage || lower.endsWith(".pdf"));
    }

    private static String inferFilenameFromContentType(String contentType, boolean requireImage) {
        if (contentType == null || contentType.isBlank()) return "file.jpg";
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("png"))  return "file.png";
        if (ct.contains("jpeg")) return "file.jpeg";
        if (ct.contains("pdf") && !requireImage) return "file.pdf";
        return "file.jpg";
    }

    private static String buildSanitizedStoredFilename(String safeName) {
        int dot = safeName.lastIndexOf('.');
        String base   = dot > 0 ? safeName.substring(0, dot) : safeName;
        String rawExt = (dot >= 0 && dot < safeName.length() - 1)
                ? safeName.substring(dot + 1) : "";
        String cleanStem = sanitizeFilenamePart(base, 80);
        if (cleanStem.isEmpty()) cleanStem = "document";
        return cleanStem + "_" + UUID.randomUUID() + "." + sanitizeExtension(rawExt);
    }

    private static String sanitizeExtension(String rawExt) {
        String part = sanitizeFilenamePart(rawExt, 12);
        return part.isEmpty() ? "bin" : part;
    }

    private static String sanitizeFilenamePart(String input, int maxLen) {
        if (input == null || input.isBlank()) return "";
        String t = input.trim().replace(' ', '_');
        t = transliterateTurkishToAscii(t).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "");
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }

    private static String transliterateTurkishToAscii(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
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
}
