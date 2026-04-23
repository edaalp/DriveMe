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
import java.util.List;
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
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (driverRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        long tckNo = request.getTckNo();
        if (String.valueOf(tckNo).length() != 11) {
            throw new IllegalArgumentException("TCK number must be 11 digits");
        }

        try {
            String hashedPassword = passwordEncoder.encode(request.getPassword());
            Driver driver = driverMapper.toEntity(request, hashedPassword);

            driver.setDriverLicenseFile(licenseFile.getBytes());
            driver.setDriverLicenseFileName(licenseFile.getOriginalFilename());

            driver.setCriminalRecordFile(criminalRecordFile.getBytes());
            driver.setCriminalRecordFileName(criminalRecordFile.getOriginalFilename());

            driver.setProfilePictureFile(selfieFile.getBytes());
            driver.setProfilePictureFileName(selfieFile.getOriginalFilename());

            Driver savedDriver = driverRepository.save(driver);
            log.info("Driver successfully signed up with ID: {}", savedDriver.getId());
            return savedDriver;
        } catch (IOException e) {
            log.error("Failed to read uploaded files", e);
            throw new IllegalArgumentException("Could not process uploaded files");
        }
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
