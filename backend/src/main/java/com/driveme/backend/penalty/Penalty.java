package com.driveme.backend.penalty;

import com.driveme.backend.common.BaseEntity;
import com.driveme.backend.common.Money;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity representing a penalty applied to a user.
 */
@Entity
@Table(name = "penalties")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Penalty extends BaseEntity {

    /**
     * The ID of the user this penalty is applied to.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The type of penalty.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PenaltyType type;

    /**
     * The penalty amount with currency.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money penaltyAmount;

    /**
     * The reason for the penalty.
     */
    @Column(length = 1000)
    private String reason;

    /**
     * Whether the penalty has been paid.
     */
    @Column(nullable = false)
    private boolean paid = false;

    /**
     * The ID of the trip associated with this penalty (if applicable).
     */
    @Column(name = "trip_id")
    private UUID tripId;

    /**
     * The ID of the payment that paid this penalty (if paid).
     */
    @Column(name = "payment_id")
    private UUID paymentId;

    /**
     * Marks the penalty as paid.
     *
     * @param paymentId the ID of the payment
     */
    public void markAsPaid(UUID paymentId) {
        this.paid = true;
        this.paymentId = paymentId;
    }

    /**
     * Applies this penalty to a user.
     * This method can be extended to include additional logic like
     * notifying the user or updating their account status.
     */
    public void applyToUser() {
        // Additional logic for applying penalty can be added here
        // For example: send notification, update user status, etc.
    }
}

