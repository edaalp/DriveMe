package com.driveme.backend.helper;

import com.driveme.backend.dto.DriverResponse;
import com.driveme.backend.entity.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mapper for converting between Driver entities and DTOs.
 */
@Component
public class DriverMapper {

    @Value("${app.public-base-url:http://192.168.1.28:8080}")
    private String publicBaseUrl;

    /**
     * Convert sign-up request DTO to Driver entity.
     *
     * @param request the sign-up request
     * @param hashedPassword the hashed password
     * @param driverLicenseDocumentUrl stored public path for license file
     * @param criminalRecordDocumentUrl stored public path for criminal record file
     * @param profilePicturePath stored public path for profile (selfie) image
     * @return the driver entity
     */
    public Driver toEntity(
            com.driveme.backend.dto.DriverSignUpRequest request,
            String hashedPassword,
            String driverLicenseDocumentUrl,
            String criminalRecordDocumentUrl,
            String profilePicturePath) {
        Driver driver = new Driver();

        driver.setEmail(request.getEmail());
        driver.setFullName(request.getFullName());
        driver.setPhoneNumber(request.getPhoneNumber());
        driver.setPasswordHash(hashedPassword);
        driver.setActive(true);

        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setVehicleDescription(request.getVehicleDescription());
        driver.setAvailable(false);
        driver.setMaxPickupRadiusKm(Optional.ofNullable(request.getMaxPickupRadiusKm()).orElse(10.0));
        driver.setMaxDropoffRadiusKm(Optional.ofNullable(request.getMaxDropoffRadiusKm()).orElse(50.0));
        driver.setAcceptsPets(Boolean.TRUE.equals(request.getAcceptsPets()));
        driver.setAvgRating(0.0);
        driver.setTckNo(request.getTckNo());
        driver.setDriverLicenseNumber(request.getDriverLicenseNumber());
        driver.setLicenseIssueDate(request.getLicenseIssueDate());
        driver.setCriminalRecordFile(null);
        driver.setCriminalRecordFileName(request.getCriminalRecordFileName());
        driver.setDriverLicenseDocumentUrl(driverLicenseDocumentUrl);
        driver.setCriminalRecordDocumentUrl(criminalRecordDocumentUrl);
        driver.setProfilePictureUrl(profilePicturePath);

        return driver;
    }

    /**
     * Convert Driver entity to response DTO.
     * Excludes sensitive information like password hash and criminal record file content.
     *
     * @param driver the driver entity
     * @return the driver response DTO
     */
    public DriverResponse toResponse(Driver driver) {
        if (driver == null) {
            return null;
        }

        DriverResponse response = new DriverResponse();
        response.setId(driver.getId());
        response.setEmail(driver.getEmail());
        response.setFullName(driver.getFullName());
        response.setPhoneNumber(driver.getPhoneNumber());
        response.setActive(driver.isActive());
        response.setLicenseNumber(driver.getLicenseNumber());
        response.setVehicleDescription(driver.getVehicleDescription());
        response.setAvailable(driver.isAvailable());
        response.setTckNo(driver.getTckNo());
        response.setDriverLicenseNumber(driver.getDriverLicenseNumber());
        response.setLicenseIssueDate(driver.getLicenseIssueDate());
        response.setCriminalRecordFileName(driver.getCriminalRecordFileName());
        response.setDriverLicenseDocumentUrl(
                toPublicUrl(normalizeStoredPath(driver.getDriverLicenseDocumentUrl(), "drivers")));
        response.setCriminalRecordDocumentUrl(
                toPublicUrl(normalizeStoredPath(driver.getCriminalRecordDocumentUrl(), "drivers")));
        response.setProfilePictureUrl(
                toPublicUrl(normalizeStoredPath(driver.getProfilePictureUrl(), null)));
        response.setVerificationStatus(driver.getVerificationStatus());
        response.setRejectionReason(driver.getRejectionReason());

        return response;
    }

    /**
     * Legacy rows may store only a filename; new rows use {@code /uploads/drivers/...} or {@code /uploads/...}.
     *
     * @param subfolderUnderUploads e.g. {@code "drivers"} for license/criminal; {@code null} for profile files at upload root
     */
    private String normalizeStoredPath(String raw, String subfolderUnderUploads) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (t.startsWith("http://") || t.startsWith("https://")) {
            return t;
        }
        if (t.startsWith("/uploads/")) {
            return t;
        }
        if (t.startsWith("uploads/")) {
            return "/" + t;
        }
        if (!t.contains("/")) {
            if (subfolderUnderUploads != null && !subfolderUnderUploads.isBlank()) {
                String s = subfolderUnderUploads.replaceAll("^/+|/+$", "");
                return "/uploads/" + s + "/" + t;
            }
            return "/uploads/" + t;
        }
        return t.startsWith("/") ? t : "/" + t;
    }

    /**
     * Turns stored paths like {@code /uploads/drivers/x.pdf} into browser-openable absolute URLs
     * using {@code app.public-base-url}.
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
