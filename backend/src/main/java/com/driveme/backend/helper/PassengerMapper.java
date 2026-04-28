package com.driveme.backend.helper;

import com.driveme.backend.entity.Passenger;
import com.driveme.backend.dto.PassengerDTO;
import com.driveme.backend.auth.PassengerSignUpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Passenger entities and DTOs.
 *
 * <p>Profile / TC photos are stored as BLOB bytes inside the {@code base_user}
 * table (see {@link Passenger}). The DTO returned to the client must therefore
 * advertise a download URL — not the raw bytes — so the Flutter app can render
 * the picture via a normal {@code NetworkImage}. We derive that URL on the fly
 * from the byte fields rather than from the legacy {@code profilePictureUrl}
 * column on the entity, because {@code PassengerService.signUp} never
 * populates that column (it only saves the bytes).
 */
@Component
public class PassengerMapper {

    /**
     * Public origin used to build absolute, browser-friendly download URLs
     * (e.g. {@code http://192.168.1.5:8080/api/passengers/{id}/document/...}).
     * Defaults to the LAN testing host; override via the
     * {@code APP_PUBLIC_BASE_URL} environment variable when running against
     * a different host (e.g. {@code http://localhost:8080} for the admin web).
     */
    @Value("${app.public-base-url:http://10.0.2.2:8080}")
    private String publicBaseUrl;

    /**
     * Convert sign-up request DTO to Passenger entity (no document URLs yet).
     */
    public Passenger toEntity(PassengerSignUpRequest request, String hashedPassword) {
        return toEntity(request, hashedPassword, null, null, null);
    }

    /**
     * Convert sign-up request DTO to Passenger entity with optional document URLs.
     *
     * @param request           the sign-up request
     * @param hashedPassword    bcrypt-hashed password
     * @param profilePictureUrl URL of the selfie (nullable)
     * @param tcPhotoFrontUrl   URL of TC front photo (nullable)
     * @param tcPhotoBackUrl    URL of TC back photo (nullable)
     */
    public Passenger toEntity(
            PassengerSignUpRequest request,
            String hashedPassword,
            String profilePictureUrl,
            String tcPhotoFrontUrl,
            String tcPhotoBackUrl) {
        Passenger passenger = new Passenger();
        passenger.setEmail(request.getEmail());
        passenger.setFullName(request.getFullName());
        passenger.setPhoneNumber(request.getPhoneNumber());
        passenger.setPasswordHash(hashedPassword);
        passenger.setActive(true);
        passenger.setTcNo(request.getTcNo());
        passenger.setProfilePictureUrl(profilePictureUrl);
        passenger.setTcPhotoFrontUrl(tcPhotoFrontUrl);
        passenger.setTcPhotoBackUrl(tcPhotoBackUrl);
        return passenger;
    }

    /**
     * Convert Passenger entity to response DTO.
     *
     * <p>Sensitive information (password hash, raw file bytes) is never
     * exposed; instead the DTO carries absolute download URLs that resolve
     * to the matching {@code @GetMapping} endpoints in
     * {@link com.driveme.backend.controller.PassengerController}.
     */
    public PassengerDTO toDTO(Passenger passenger) {
        if (passenger == null) {
            return null;
        }

        PassengerDTO response = new PassengerDTO();
        response.setId(passenger.getId());
        response.setEmail(passenger.getEmail());
        response.setFullName(passenger.getFullName());
        response.setPhoneNumber(passenger.getPhoneNumber());
        response.setActive(passenger.isActive());
        response.setTcNo(passenger.getTcNo());

        response.setProfilePictureUrl(
                buildDocumentUrl(passenger, "profile-picture",
                        passenger.getProfilePictureFile(),
                        passenger.getProfilePictureUrl()));
        response.setTcPhotoFrontUrl(
                buildDocumentUrl(passenger, "tc-front",
                        passenger.getTcPhotoFrontFile(),
                        passenger.getTcPhotoFrontUrl()));
        response.setTcPhotoBackUrl(
                buildDocumentUrl(passenger, "tc-back",
                        passenger.getTcPhotoBackFile(),
                        passenger.getTcPhotoBackUrl()));

        return response;
    }

    /**
     * Build an absolute, downloadable URL for a passenger document.
     *
     * <p>Resolution order (first match wins):
     * <ol>
     *   <li>If the entity already carries a URL (legacy or admin-uploaded),
     *       just normalise it to absolute form via {@code app.public-base-url}.</li>
     *   <li>If the document is stored as bytes in the database, point at the
     *       matching {@code GET /api/passengers/{id}/document/...} endpoint.</li>
     *   <li>Otherwise, return {@code null} so the client can render its own
     *       fallback (e.g. an avatar placeholder).</li>
     * </ol>
     */
    private String buildDocumentUrl(
            Passenger passenger, String documentSegment,
            byte[] storedBytes, String legacyUrl) {
        if (legacyUrl != null && !legacyUrl.isBlank()) {
            return toPublicUrl(legacyUrl);
        }
        if (storedBytes != null && storedBytes.length > 0 && passenger.getId() != null) {
            return toPublicUrl(
                    "/api/passengers/" + passenger.getId()
                            + "/document/" + documentSegment);
        }
        return null;
    }

    /**
     * Turn a relative path like {@code /api/passengers/.../document/profile-picture}
     * into an absolute URL using {@code app.public-base-url}. Already-absolute
     * URLs are returned untouched.
     */
    private String toPublicUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return null;
        }
        String trimmed = pathOrUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        String path = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return base + path;
    }
}
