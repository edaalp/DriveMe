package com.driveme.backend.payment;

import com.driveme.backend.common.Money;
import com.driveme.backend.payment.dto.CreatePaymentRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing payments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * Creates a new payment.
     *
     * @param request the payment creation request
     * @return the created payment
     */
    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for user: {}", request.getUserId());

        Payment payment = new Payment();
        payment.setTripId(request.getTripId());
        payment.setUserId(request.getUserId());
        payment.setPaymentAmount(new Money(request.getAmount(), request.getCurrency()));
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setDueBy(request.getDueBy());
        payment.setDescription(request.getDescription());

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", savedPayment.getId());

        return savedPayment;
    }

    /**
     * Gets a payment by ID.
     *
     * @param paymentId the payment ID
     * @return optional containing the payment if found
     */
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId);
    }

    /**
     * Gets all payments for a user.
     *
     * @param userId the user ID
     * @return list of payments
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByUserId(UUID userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Gets all payments for a trip.
     *
     * @param tripId the trip ID
     * @return list of payments
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByTripId(UUID tripId) {
        return paymentRepository.findByTripId(tripId);
    }

    /**
     * Gets all payments by status.
     *
     * @param status the payment status
     * @return list of payments
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    /**
     * Gets pending payments for a user.
     *
     * @param userId the user ID
     * @return list of pending payments
     */
    @Transactional(readOnly = true)
    public List<Payment> getPendingPaymentsByUserId(UUID userId) {
        return paymentRepository.findByUserIdAndStatus(userId, PaymentStatus.PENDING);
    }

    /**
     * Confirms a payment.
     *
     * @param paymentId the payment ID
     * @return the confirmed payment
     * @throws IllegalArgumentException if payment not found
     * @throws IllegalStateException if payment is not in PENDING status
     */
    @Transactional
    public Payment confirmPayment(UUID paymentId) {
        log.info("Confirming payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment is not in PENDING status. Current status: " + payment.getStatus());
        }

        payment.confirm();
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment confirmed: {}", paymentId);
        return savedPayment;
    }

    /**
     * Fails a payment.
     *
     * @param paymentId the payment ID
     * @return the failed payment
     * @throws IllegalArgumentException if payment not found
     */
    @Transactional
    public Payment failPayment(UUID paymentId) {
        log.info("Failing payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment is not in PENDING status. Current status: " + payment.getStatus());
        }

        payment.fail();
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment failed: {}", paymentId);
        return savedPayment;
    }

    /**
     * Cancels a payment.
     *
     * @param paymentId the payment ID
     * @return the cancelled payment
     * @throws IllegalArgumentException if payment not found
     */
    @Transactional
    public Payment cancelPayment(UUID paymentId) {
        log.info("Cancelling payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel a confirmed payment");
        }

        payment.cancel();
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment cancelled: {}", paymentId);
        return savedPayment;
    }

    /**
     * Gets all overdue payments.
     *
     * @return list of overdue payments
     */
    @Transactional(readOnly = true)
    public List<Payment> getOverduePayments() {
        return paymentRepository.findOverduePayments(Instant.now());
    }

    /**
     * Checks if a trip has a confirmed payment.
     *
     * @param tripId the trip ID
     * @return true if a confirmed payment exists
     */
    @Transactional(readOnly = true)
    public boolean hasTripConfirmedPayment(UUID tripId) {
        return paymentRepository.existsByTripIdAndStatus(tripId, PaymentStatus.CONFIRMED);
    }
}

