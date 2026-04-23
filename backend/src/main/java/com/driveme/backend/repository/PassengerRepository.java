package com.driveme.backend.repository;

import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Passenger entity.
 */
@Repository
public interface PassengerRepository extends JpaRepository<Passenger, UUID> {

    /**
     * Find passenger by email.
     */
    Optional<Passenger> findByEmail(String email);

    /**
     * Find passenger by username.
     */
    Optional<Passenger> findByUserName(String userName);

    /**
     * Find active passengers.
     */
    List<Passenger> findByActiveTrue();

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Check if username exists.
     */
    boolean existsByUserName(String userName);

    /**
     * Find passengers by verification status (admin review).
     */
    List<Passenger> findByVerificationStatus(VerificationStatus verificationStatus);

    @Query("SELECT COUNT(p) FROM Passenger p WHERE p.verificationStatus IS NULL OR p.verificationStatus = 'PENDING'")
    long countPendingVerification();
}
