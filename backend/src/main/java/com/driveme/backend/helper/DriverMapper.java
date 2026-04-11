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

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    /**
     * Convert sign-up request DTO to Driver entity.
     *
     * @param request the sign-up request
     * @param hashedPassword the hashed password
     * @param driverLicenseDocumentUrl stored public path for license file
     * @param criminalRecordDocumentUrl stored public path for criminal record file
     * @return the driver entity
     */
    public Driver toEntity(
            com.driveme.backend.dto.DriverSignUpRequest request,
            String hashedPassword,
            String driverLicenseDocumentUrl,
            String criminalRecordDocumentUrl) {
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
                toPublicUrl(driver.getDriverLicenseDocumentUrl()));
        response.setCriminalRecordDocumentUrl(
                toPublicUrl(driver.getCriminalRecordDocumentUrl()));
        response.setVerificationStatus(driver.getVerificationStatus());
        response.setRejectionReason(driver.getRejectionReason());

        return response;
    }

    /**
     * Turns stored paths like {@code /uploads/drivers/x.pdf} into browser-openable absolute URLs.
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
