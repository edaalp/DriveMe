package com.driveme.backend.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment entity operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find all payments by user ID.
     *
     * @param userId the user ID
     * @return list of payments
     */
    List<Payment> findByUserId(UUID userId);

    /**
     * Find all payments by trip ID.
     *
     * @param tripId the trip ID
     * @return list of payments
     */
    List<Payment> findByTripId(UUID tripId);

    /**
     * Find all payments by status.
     *
     * @param status the payment status
     * @return list of payments
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find all payments by user ID and status.
     *
     * @param userId the user ID
     * @param status the payment status
     * @return list of payments
     */
    List<Payment> findByUserIdAndStatus(UUID userId, PaymentStatus status);

    /**
     * Find payment by trip ID and user ID.
     *
     * @param tripId the trip ID
     * @param userId the user ID
     * @return optional payment
     */
    Optional<Payment> findByTripIdAndUserId(UUID tripId, UUID userId);

    /**
     * Find all overdue payments (past due date and still pending).
     *
     * @param now current timestamp
     * @return list of overdue payments
     */
    @Query("SELECT p FROM Payment p WHERE p.dueBy < :now AND p.status = 'PENDING'")
    List<Payment> findOverduePayments(@Param("now") Instant now);

    /**
     * Find all payments by user ID ordered by creation date descending.
     *
     * @param userId the user ID
     * @return list of payments sorted by newest first
     */
    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Check if a confirmed payment exists for a trip.
     *
     * @param tripId the trip ID
     * @return true if a confirmed payment exists
     */
    boolean existsByTripIdAndStatus(UUID tripId, PaymentStatus status);
}

