package com.driveme.backend.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

    /**
     * Register a new passenger.
     * 
     * @param request the sign-up request
     * @return the created passenger
     * @throws IllegalArgumentException if email or username already exists
     */
    @Transactional
    public Passenger signUp(PassengerSignUpRequest request) {
        log.info("Attempting to sign up passenger with email: {}", request.getEmail());

        // Check if email already exists
        if (passengerRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Sign up failed: Email already exists - {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new passenger passwordEncoder.encode
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        Passenger passenger = passengerMapper.toEntity(request, hashedPassword);

        Passenger savedPassenger = passengerRepository.save(passenger);
        log.info("Passenger successfully signed up with ID: {}", savedPassenger.getId());

        return savedPassenger;
    }

    /**
     * Find passenger by email.
     * 
     * @param email the email to search for
     * @return optional containing the passenger if found
     */
    public Optional<Passenger> findByEmail(String email) {
        return passengerRepository.findByEmail(email);
    }
}
