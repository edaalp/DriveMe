package com.driveme.backend.controller;

import com.driveme.backend.dto.CreatePaymentRequest;
import com.driveme.backend.dto.PaymentResponse;
import com.driveme.backend.entity.Payment;
import com.driveme.backend.helper.PaymentMapper;
import com.driveme.backend.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for payment operations.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    /**
     * Creates a new payment.
     *
     * @param request the payment creation request
     * @return the created payment
     */
    @PostMapping
    @Operation(summary = "Create a new payment", description = "Creates a new payment record")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Payment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("Creating payment for user: {}", request.getUserId());
        Payment payment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMapper.toResponse(payment));
    }

    /**
     * Gets a payment by ID.
     *
     * @param paymentId the payment ID
     * @return the payment if found
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieves a payment by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId) {
        return paymentService.getPaymentById(paymentId)
                .map(payment -> ResponseEntity.ok(paymentMapper.toResponse(payment)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets all payments for a user.
     *
     * @param userId the user ID
     * @return list of payments
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get payments by user", description = "Retrieves all payments for a specific user")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByUserId(userId)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payments);
    }

    /**
     * Gets all payments for a trip.
     *
     * @param tripId the trip ID
     * @return list of payments
     */
    @GetMapping("/trip/{tripId}")
    @Operation(summary = "Get payments by trip", description = "Retrieves all payments for a specific trip")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByTrip(
            @Parameter(description = "Trip ID") @PathVariable UUID tripId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByTripId(tripId)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payments);
    }

    /**
     * Gets all pending payments for a user.
     *
     * @param userId the user ID
     * @return list of pending payments
     */
    @GetMapping("/user/{userId}/pending")
    @Operation(summary = "Get pending payments", description = "Retrieves all pending payments for a user")
    public ResponseEntity<List<PaymentResponse>> getPendingPayments(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        List<PaymentResponse> payments = paymentService.getPendingPaymentsByUserId(userId)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payments);
    }

    /**
     * Gets all overdue payments.
     *
     * @return list of overdue payments
     */
    @GetMapping("/overdue")
    @Operation(summary = "Get overdue payments", description = "Retrieves all overdue payments")
    public ResponseEntity<List<PaymentResponse>> getOverduePayments() {
        List<PaymentResponse> payments = paymentService.getOverduePayments()
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payments);
    }

    /**
     * Confirms a payment.
     *
     * @param paymentId the payment ID
     * @return the confirmed payment
     */
    @PostMapping("/{paymentId}/confirm")
    @Operation(summary = "Confirm a payment", description = "Confirms a pending payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment confirmed"),
        @ApiResponse(responseCode = "400", description = "Payment cannot be confirmed"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<?> confirmPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId) {
        try {
            Payment payment = paymentService.confirmPayment(paymentId);
            return ResponseEntity.ok(paymentMapper.toResponse(payment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Fails a payment.
     *
     * @param paymentId the payment ID
     * @return the failed payment
     */
    @PostMapping("/{paymentId}/fail")
    @Operation(summary = "Fail a payment", description = "Marks a payment as failed")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment marked as failed"),
        @ApiResponse(responseCode = "400", description = "Payment cannot be failed"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<?> failPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId) {
        try {
            Payment payment = paymentService.failPayment(paymentId);
            return ResponseEntity.ok(paymentMapper.toResponse(payment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Cancels a payment.
     *
     * @param paymentId the payment ID
     * @return the cancelled payment
     */
    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Cancel a payment", description = "Cancels a pending payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment cancelled"),
        @ApiResponse(responseCode = "400", description = "Payment cannot be cancelled"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<?> cancelPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId) {
        try {
            Payment payment = paymentService.cancelPayment(paymentId);
            return ResponseEntity.ok(paymentMapper.toResponse(payment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Simple error response class.
     */
    private record ErrorResponse(String message) {}
}

