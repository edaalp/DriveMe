package com.driveme.backend.penalty;

import com.driveme.backend.common.Money;
import com.driveme.backend.penalty.dto.CreatePenaltyRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing penalties.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;

    /**
     * Creates a new penalty.
     *
     * @param request the penalty creation request
     * @return the created penalty
     */
    @Transactional
    public Penalty createPenalty(CreatePenaltyRequest request) {
        log.info("Creating penalty for user: {} of type: {}", request.getUserId(), request.getType());

        Penalty penalty = new Penalty();
        penalty.setUserId(request.getUserId());
        penalty.setType(request.getType());
        penalty.setPenaltyAmount(new Money(request.getAmount(), request.getCurrency()));
        penalty.setReason(request.getReason());
        penalty.setTripId(request.getTripId());
        penalty.setPaid(false);

        Penalty savedPenalty = penaltyRepository.save(penalty);
        log.info("Penalty created with ID: {}", savedPenalty.getId());

        // Apply penalty to user (can trigger notifications, etc.)
        savedPenalty.applyToUser();

        return savedPenalty;
    }

    /**
     * Gets a penalty by ID.
     *
     * @param penaltyId the penalty ID
     * @return optional containing the penalty if found
     */
    @Transactional(readOnly = true)
    public Optional<Penalty> getPenaltyById(UUID penaltyId) {
        return penaltyRepository.findById(penaltyId);
    }

    /**
     * Gets all penalties for a user.
     *
     * @param userId the user ID
     * @return list of penalties
     */
    @Transactional(readOnly = true)
    public List<Penalty> getPenaltiesByUserId(UUID userId) {
        return penaltyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Gets unpaid penalties for a user.
     *
     * @param userId the user ID
     * @return list of unpaid penalties
     */
    @Transactional(readOnly = true)
    public List<Penalty> getUnpaidPenaltiesByUserId(UUID userId) {
        return penaltyRepository.findByUserIdAndPaidFalse(userId);
    }

    /**
     * Gets paid penalties for a user.
     *
     * @param userId the user ID
     * @return list of paid penalties
     */
    @Transactional(readOnly = true)
    public List<Penalty> getPaidPenaltiesByUserId(UUID userId) {
        return penaltyRepository.findByUserIdAndPaidTrue(userId);
    }

    /**
     * Gets all penalties by type.
     *
     * @param type the penalty type
     * @return list of penalties
     */
    @Transactional(readOnly = true)
    public List<Penalty> getPenaltiesByType(PenaltyType type) {
        return penaltyRepository.findByType(type);
    }

    /**
     * Gets penalties by trip ID.
     *
     * @param tripId the trip ID
     * @return list of penalties
     */
    @Transactional(readOnly = true)
    public List<Penalty> getPenaltiesByTripId(UUID tripId) {
        return penaltyRepository.findByTripId(tripId);
    }

    /**
     * Marks a penalty as paid.
     *
     * @param penaltyId the penalty ID
     * @param paymentId the payment ID
     * @return the updated penalty
     * @throws IllegalArgumentException if penalty not found
     * @throws IllegalStateException if penalty is already paid
     */
    @Transactional
    public Penalty markPenaltyAsPaid(UUID penaltyId, UUID paymentId) {
        log.info("Marking penalty {} as paid with payment {}", penaltyId, paymentId);

        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalArgumentException("Penalty not found: " + penaltyId));

        if (penalty.isPaid()) {
            throw new IllegalStateException("Penalty is already paid");
        }

        penalty.markAsPaid(paymentId);
        Penalty savedPenalty = penaltyRepository.save(penalty);

        log.info("Penalty {} marked as paid", penaltyId);
        return savedPenalty;
    }

    /**
     * Gets the count of unpaid penalties for a user.
     *
     * @param userId the user ID
     * @return count of unpaid penalties
     */
    @Transactional(readOnly = true)
    public long getUnpaidPenaltyCount(UUID userId) {
        return penaltyRepository.countByUserIdAndPaidFalse(userId);
    }

    /**
     * Gets the total unpaid penalty amount for a user.
     *
     * @param userId the user ID
     * @return total unpaid amount
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalUnpaidAmount(UUID userId) {
        return penaltyRepository.getTotalUnpaidAmountByUserId(userId);
    }

    /**
     * Checks if a user has any unpaid penalties.
     *
     * @param userId the user ID
     * @return true if user has unpaid penalties
     */
    @Transactional(readOnly = true)
    public boolean hasUnpaidPenalties(UUID userId) {
        return penaltyRepository.existsByUserIdAndPaidFalse(userId);
    }

    /**
     * Deletes a penalty.
     * This should only be used for administrative purposes.
     *
     * @param penaltyId the penalty ID
     * @throws IllegalArgumentException if penalty not found
     */
    @Transactional
    public void deletePenalty(UUID penaltyId) {
        log.info("Deleting penalty: {}", penaltyId);

        if (!penaltyRepository.existsById(penaltyId)) {
            throw new IllegalArgumentException("Penalty not found: " + penaltyId);
        }

        penaltyRepository.deleteById(penaltyId);
        log.info("Penalty deleted: {}", penaltyId);
    }
}

