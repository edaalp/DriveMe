package com.driveme.backend.service;

import com.driveme.backend.dto.DriverSignUpRequest;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.helper.DriverMapper;
import com.driveme.backend.repository.DriverRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

    /**
     * Register a new driver.
     * 
     * @param request the sign-up request
     * @return the created driver
     * @throws IllegalArgumentException if email already exists or passwords don't match
     */
    @Transactional
    public Driver signUp(@Valid  DriverSignUpRequest request) {
        //removed @org.checkerframework.checker.nullness.qual.MonotonicNonNull
        log.info("Attempting to sign up driver with email: {}", request.getEmail());

        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("Sign up failed: Passwords do not match");
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if email already exists
        if (driverRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Sign up failed: Email already exists - {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        // Validate TCK number (should be 11 digits)
        Long tckNo = request.getTckNo();
        if (tckNo != null && String.valueOf(tckNo).length() != 11) {
            log.warn("Sign up failed: Invalid TCK number");
            throw new IllegalArgumentException("TCK number must be 11 digits");
        }

        // Create new driver
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        Driver driver = driverMapper.toEntity(request, hashedPassword);

        Driver savedDriver = driverRepository.save(driver);
        log.info("Driver successfully signed up with ID: {}", savedDriver.getId());

        return savedDriver;
    }

    /**
     * Find driver by email.
     * 
     * @param email the email to search for
     * @return optional containing the driver if found
     */
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
}
