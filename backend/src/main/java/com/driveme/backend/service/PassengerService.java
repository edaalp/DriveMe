package com.driveme.backend.service;

import com.driveme.backend.auth.PassengerSignUpRequest;
import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.dto.PassengerAdminResponse;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.helper.PassengerMapper;
import com.driveme.backend.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * Register a new passenger (selfie + identity document stored in DB, same as driver KYC).
     */
    @Transactional
    public Passenger signUp(
            PassengerSignUpRequest request,
            MultipartFile selfieFile,
            MultipartFile identityDocumentFile) {
        log.info("Attempting to sign up passenger with email: {}", request.getEmail());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (selfieFile == null || selfieFile.isEmpty()) {
            throw new IllegalArgumentException("Selfie photo is required");
        }
        if (identityDocumentFile == null || identityDocumentFile.isEmpty()) {
            throw new IllegalArgumentException("Identity document is required");
        }

        if (passengerRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Sign up failed: Email already exists - {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        try {
            String hashedPassword = passwordEncoder.encode(request.getPassword());
            Passenger passenger = passengerMapper.toEntity(request, hashedPassword);

            byte[] selfieBytes = selfieFile.getBytes();
            byte[] idBytes = identityDocumentFile.getBytes();
            log.info(
                    "Saving passenger files: selfieLen={} identityLen={}",
                    selfieBytes.length,
                    idBytes.length);

            passenger.setProfilePictureFile(selfieBytes);
            passenger.setProfilePictureFileName(
                    selfieFile.getOriginalFilename() != null ? selfieFile.getOriginalFilename() : "selfie.jpg");

            passenger.setIdentityDocumentFile(idBytes);
            passenger.setIdentityDocumentFileName(
                    identityDocumentFile.getOriginalFilename() != null
                            ? identityDocumentFile.getOriginalFilename()
                            : "identity.pdf");

            Passenger savedPassenger = passengerRepository.save(passenger);
            log.info("Passenger successfully signed up with ID: {}", savedPassenger.getId());
            return savedPassenger;
        } catch (IOException e) {
            log.error("Failed to read uploaded passenger files", e);
            throw new IllegalArgumentException("Could not process uploaded files");
        }
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

    // ---- Admin ----

    @Transactional(readOnly = true)
    public List<PassengerAdminResponse> getPassengersByVerificationStatus(VerificationStatus status) {
        if (status == VerificationStatus.PENDING) {
            return passengerRepository.findAll().stream()
                    .filter(p -> p.getVerificationStatus() == null
                            || p.getVerificationStatus() == VerificationStatus.PENDING)
                    .map(passengerMapper::toAdminResponse)
                    .collect(Collectors.toList());
        }
        if (status == VerificationStatus.VERIFIED) {
            return passengerRepository.findByVerificationStatus(VerificationStatus.VERIFIED).stream()
                    .map(passengerMapper::toAdminResponse)
                    .collect(Collectors.toList());
        }
        return passengerRepository.findByVerificationStatus(status).stream()
                .map(passengerMapper::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countPendingPassengers() {
        return passengerRepository.countPendingVerification();
    }

    @Transactional(readOnly = true)
    public List<PassengerAdminResponse> getAllPassengersForAdmin() {
        return passengerRepository.findAll().stream()
                .map(passengerMapper::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PassengerAdminResponse verifyPassenger(UUID passengerId, VerificationStatus decision, String reason) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found with id: " + passengerId));

        if (decision == VerificationStatus.PENDING) {
            throw new IllegalArgumentException("Decision must be VERIFIED or REJECTED");
        }

        passenger.setVerificationStatus(decision);
        passenger.setRejectionReason(decision == VerificationStatus.REJECTED ? reason : null);

        Passenger saved = passengerRepository.save(passenger);
        log.info("Passenger {} verification updated to: {}", passengerId, decision);
        return passengerMapper.toAdminResponse(saved);
    }

    @Transactional(readOnly = true)
    public Passenger getPassengerEntityById(UUID passengerId) {
        return passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found with id: " + passengerId));
    }
}
