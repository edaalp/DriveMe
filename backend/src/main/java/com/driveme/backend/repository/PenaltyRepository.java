package com.driveme.backend.repository;

import com.driveme.backend.common.PenaltyType;
import com.driveme.backend.entity.Penalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Penalty entity operations.
 */
@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, UUID> {

    /**
     * Find all penalties by user ID.
     *
     * @param userId the user ID
     * @return list of penalties
     */
    List<Penalty> findByUserId(UUID userId);

    /**
     * Find all penalties by user ID ordered by creation date descending.
     *
     * @param userId the user ID
     * @return list of penalties sorted by newest first
     */
    List<Penalty> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find all penalties by type.
     *
     * @param type the penalty type
     * @return list of penalties
     */
    List<Penalty> findByType(PenaltyType type);

    /**
     * Find all unpaid penalties by user ID.
     *
     * @param userId the user ID
     * @return list of unpaid penalties
     */
    List<Penalty> findByUserIdAndPaidFalse(UUID userId);

    /**
     * Find all paid penalties by user ID.
     *
     * @param userId the user ID
     * @return list of paid penalties
     */
    List<Penalty> findByUserIdAndPaidTrue(UUID userId);

    /**
     * Find penalties by trip ID.
     *
     * @param tripId the trip ID
     * @return list of penalties
     */
    List<Penalty> findByTripId(UUID tripId);

    /**
     * Count unpaid penalties for a user.
     *
     * @param userId the user ID
     * @return count of unpaid penalties
     */
    long countByUserIdAndPaidFalse(UUID userId);

    /**
     * Calculate total unpaid penalty amount for a user.
     *
     * @param userId the user ID
     * @return total unpaid amount
     */
    @Query("SELECT COALESCE(SUM(p.penaltyAmount.amount), 0) FROM Penalty p WHERE p.userId = :userId AND p.paid = false")
    BigDecimal getTotalUnpaidAmountByUserId(@Param("userId") UUID userId);

    /**
     * Check if user has any unpaid penalties.
     *
     * @param userId the user ID
     * @return true if user has unpaid penalties
     */
    boolean existsByUserIdAndPaidFalse(UUID userId);
}

