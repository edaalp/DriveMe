package com.driveme.backend.service;

import com.driveme.backend.config.JwtUtil;
import com.driveme.backend.dto.LoginRequest;
import com.driveme.backend.dto.LoginResponse;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.repository.DriverRepository;
import com.driveme.backend.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for handling authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Authenticate user and generate JWT token.
     *
     * @param request login request with email and password
     * @return login response with token and user details
     * @throws IllegalArgumentException if credentials are invalid
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Attempting to authenticate user with email: {}", request.getEmail());

        // Try to find user as driver first
        Optional<Driver> driverOpt = driverRepository.findByEmail(request.getEmail());
        if (driverOpt.isPresent()) {
            Driver driver = driverOpt.get();
            
            // Check if user is active
            if (!driver.isActive()) {
                log.warn("Login failed: Driver account is inactive - {}", request.getEmail());
                throw new IllegalArgumentException("Account is inactive");
            }
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), driver.getPasswordHash())) {
                log.warn("Login failed: Invalid password for driver - {}", request.getEmail());
                throw new IllegalArgumentException("Invalid email or password");
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(
                driver.getEmail(), 
                driver.getId().toString(), 
                "DRIVER"
            );

            log.info("Driver successfully authenticated: {}", driver.getId());
            
            return LoginResponse.builder()
                    .token(token)
                    .userType("DRIVER")
                    .userId(driver.getId().toString())
                    .email(driver.getEmail())
                    .fullName(driver.getFullName())
                    .userName(driver.getUserName())
                    .build();
        }

        // Try to find user as passenger
        Optional<Passenger> passengerOpt = passengerRepository.findByEmail(request.getEmail());
        if (passengerOpt.isPresent()) {
            Passenger passenger = passengerOpt.get();
            
            // Check if user is active
            if (!passenger.isActive()) {
                log.warn("Login failed: Passenger account is inactive - {}", request.getEmail());
                throw new IllegalArgumentException("Account is inactive");
            }
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), passenger.getPasswordHash())) {
                log.warn("Login failed: Invalid password for passenger - {}", request.getEmail());
                throw new IllegalArgumentException("Invalid email or password");
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(
                passenger.getEmail(), 
                passenger.getId().toString(), 
                "PASSENGER"
            );

            log.info("Passenger successfully authenticated: {}", passenger.getId());
            
            return LoginResponse.builder()
                    .token(token)
                    .userType("PASSENGER")
                    .userId(passenger.getId().toString())
                    .email(passenger.getEmail())
                    .fullName(passenger.getFullName())
                    .userName(passenger.getUserName())
                    .build();
        }

        // User not found
        log.warn("Login failed: User not found - {}", request.getEmail());
        throw new IllegalArgumentException("Invalid email or password");
    }
}
