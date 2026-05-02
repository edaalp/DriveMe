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

    public Driver toEntity(
            com.driveme.backend.dto.DriverSignUpRequest request,
            String hashedPassword) {
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

        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        String idStr = driver.getId().toString();

        DriverResponse response = new DriverResponse();
        response.setId(driver.getId());
        response.setEmail(driver.getEmail());
        response.setFullName(driver.getFullName());
        response.setPhoneNumber(driver.getPhoneNumber());
        response.setActive(driver.isActive());
        response.setLicenseNumber(driver.getLicenseNumber());
        response.setVehicleDescription(driver.getVehicleDescription());
        response.setAvailable(driver.isAvailable());
        response.setAcceptsPets(driver.isAcceptsPets());
        response.setTckNo(driver.getTckNo());
        response.setDriverLicenseNumber(driver.getDriverLicenseNumber());
        response.setLicenseIssueDate(driver.getLicenseIssueDate());
        response.setCriminalRecordFileName(driver.getCriminalRecordFileName());

        response.setDriverLicenseDocumentUrl(
                driver.getDriverLicenseFile() != null
                        ? base + "/api/admin/drivers/" + idStr + "/document/license"
                        : null);
        response.setCriminalRecordDocumentUrl(
                driver.getCriminalRecordFile() != null
                        ? base + "/api/admin/drivers/" + idStr + "/document/criminal-record"
                        : null);
        response.setProfilePictureUrl(
                driver.getProfilePictureFile() != null
                        ? base + "/api/admin/drivers/" + idStr + "/document/profile-picture"
                        : null);

        response.setVerificationStatus(driver.getVerificationStatus());
        response.setRejectionReason(driver.getRejectionReason());

        return response;
    }
}
