package com.driveme.backend.entity;

import com.driveme.backend.common.Money;
import com.driveme.backend.common.PaymentMethod;
import com.driveme.backend.common.PaymentStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a payment for a trip.
 */
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    /**
     * The ID of the trip this payment is for.
     */
    @Column(name = "trip_id")
    private UUID tripId;

    /**
     * The ID of the user who made the payment.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The payment amount with currency.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money paymentAmount;

    /**
     * The method used for payment (CASH, BANK_TRANSFER).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    /**
     * The current status of the payment.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * The due date for the payment.
     */
    @Column(name = "due_by")
    private Instant dueBy;

    /**
     * The timestamp when the payment was confirmed.
     */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /**
     * Optional description or reference for the payment.
     */
    @Column(length = 500)
    private String description;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Marks the payment with a due date.
     *
     * @param dueBy the due date
     */
    public void markDue(Instant dueBy) {
        this.dueBy = dueBy;
    }

    /**
     * Confirms the payment.
     * Sets status to CONFIRMED and records the confirmation timestamp.
     */
    public void confirm() {
        this.status = PaymentStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    /**
     * Marks the payment as failed.
     */
    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    /**
     * Cancels the payment.
     */
    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }

    /**
     * Checks if the payment is overdue.
     *
     * @return true if the payment is past its due date and not confirmed
     */
    public boolean isOverdue() {
        return dueBy != null 
            && Instant.now().isAfter(dueBy) 
            && status == PaymentStatus.PENDING;
    }
}
