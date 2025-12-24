package com.driveme.backend.helper;

import com.driveme.backend.entity.Driver;
import org.springframework.stereotype.Component;
import com.driveme.backend.dto.DriverResponse;
import java.util.Base64;

/**
 * Mapper for converting between Driver entities and DTOs.
 */
@Component
public class DriverMapper {

    /**
     * Convert sign-up request DTO to Driver entity.
     * 
     * @param request the sign-up request
     * @param hashedPassword the hashed password
     * @return the driver entity
     */
    public Driver toEntity(com.driveme.backend.dto.DriverSignUpRequest request, String hashedPassword) {
        Driver driver = new Driver();
        
        // Set BaseUser fields
        driver.setEmail(request.getEmail());
        driver.setFullName(request.getFullName());
        driver.setPhoneNumber(request.getPhoneNumber());
        driver.setPasswordHash(hashedPassword);
        driver.setActive(true);
        
        // Set Driver-specific fields
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setAvailable(false); // defaults to false for new drivers
        driver.setAvgRating(0.0); // starts at 0
        driver.setTckNo(request.getTckNo());
        driver.setDriverLicenseNumber(request.getDriverLicenseNumber());
        driver.setLicanseIssueDate(request.getLicenseIssueDate());
        driver.setCriminalRecordFile(decodeBase64File(request.getCriminalRecordFile()));
        driver.setCriminalRecordFileName(request.getCriminalRecordFileName());
        
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
        response.setAvailable(driver.isAvailable());
        response.setTckNo(driver.getTckNo());
        response.setDriverLicenseNumber(driver.getDriverLicenseNumber());
        response.setLicenseIssueDate(driver.getLicanseIssueDate());
        response.setCriminalRecordFileName(driver.getCriminalRecordFileName());
        
        return response;
    }

    /**
     * Decode Base64 encoded file content.
     * 
     * @param base64String the Base64 encoded string
     * @return decoded byte array, or null if input is null/empty
     */
    private byte[] decodeBase64File(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(base64String);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoded file content");
        }
    }
}
