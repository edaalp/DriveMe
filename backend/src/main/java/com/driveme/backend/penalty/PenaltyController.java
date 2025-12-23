package com.driveme.backend.penalty;

import com.driveme.backend.penalty.dto.CreatePenaltyRequest;
import com.driveme.backend.penalty.dto.PenaltyResponse;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for penalty operations.
 */
@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Penalty", description = "Penalty management APIs")
public class PenaltyController {

    private final PenaltyService penaltyService;
    private final PenaltyMapper penaltyMapper;

    /**
     * Creates a new penalty.
     *
     * @param request the penalty creation request
     * @return the created penalty
     */
    @PostMapping
    @Operation(summary = "Create a new penalty", description = "Creates a new penalty record for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Penalty created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<PenaltyResponse> createPenalty(@Valid @RequestBody CreatePenaltyRequest request) {
        log.info("Creating penalty for user: {}", request.getUserId());
        Penalty penalty = penaltyService.createPenalty(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(penaltyMapper.toResponse(penalty));
    }

    /**
     * Gets a penalty by ID.
     *
     * @param penaltyId the penalty ID
     * @return the penalty if found
     */
    @GetMapping("/{penaltyId}")
    @Operation(summary = "Get penalty by ID", description = "Retrieves a penalty by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Penalty found"),
        @ApiResponse(responseCode = "404", description = "Penalty not found")
    })
    public ResponseEntity<PenaltyResponse> getPenalty(
            @Parameter(description = "Penalty ID") @PathVariable UUID penaltyId) {
        return penaltyService.getPenaltyById(penaltyId)
                .map(penalty -> ResponseEntity.ok(penaltyMapper.toResponse(penalty)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets all penalties for a user.
     *
     * @param userId the user ID
     * @return list of penalties
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get penalties by user", description = "Retrieves all penalties for a specific user")
    public ResponseEntity<List<PenaltyResponse>> getPenaltiesByUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        List<PenaltyResponse> penalties = penaltyService.getPenaltiesByUserId(userId)
                .stream()
                .map(penaltyMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(penalties);
    }

    /**
     * Gets unpaid penalties for a user.
     *
     * @param userId the user ID
     * @return list of unpaid penalties
     */
    @GetMapping("/user/{userId}/unpaid")
    @Operation(summary = "Get unpaid penalties", description = "Retrieves all unpaid penalties for a user")
    public ResponseEntity<List<PenaltyResponse>> getUnpaidPenalties(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        List<PenaltyResponse> penalties = penaltyService.getUnpaidPenaltiesByUserId(userId)
                .stream()
                .map(penaltyMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(penalties);
    }

    /**
     * Gets paid penalties for a user.
     *
     * @param userId the user ID
     * @return list of paid penalties
     */
    @GetMapping("/user/{userId}/paid")
    @Operation(summary = "Get paid penalties", description = "Retrieves all paid penalties for a user")
    public ResponseEntity<List<PenaltyResponse>> getPaidPenalties(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        List<PenaltyResponse> penalties = penaltyService.getPaidPenaltiesByUserId(userId)
                .stream()
                .map(penaltyMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(penalties);
    }

    /**
     * Gets penalties by trip ID.
     *
     * @param tripId the trip ID
     * @return list of penalties
     */
    @GetMapping("/trip/{tripId}")
    @Operation(summary = "Get penalties by trip", description = "Retrieves all penalties for a specific trip")
    public ResponseEntity<List<PenaltyResponse>> getPenaltiesByTrip(
            @Parameter(description = "Trip ID") @PathVariable UUID tripId) {
        List<PenaltyResponse> penalties = penaltyService.getPenaltiesByTripId(tripId)
                .stream()
                .map(penaltyMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(penalties);
    }

    /**
     * Gets the total unpaid penalty amount for a user.
     *
     * @param userId the user ID
     * @return summary of unpaid penalties
     */
    @GetMapping("/user/{userId}/summary")
    @Operation(summary = "Get penalty summary", description = "Gets summary of user's penalty status")
    public ResponseEntity<PenaltySummary> getPenaltySummary(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        long count = penaltyService.getUnpaidPenaltyCount(userId);
        BigDecimal totalAmount = penaltyService.getTotalUnpaidAmount(userId);
        boolean hasUnpaid = penaltyService.hasUnpaidPenalties(userId);

        PenaltySummary summary = new PenaltySummary(userId, count, totalAmount, hasUnpaid);
        return ResponseEntity.ok(summary);
    }

    /**
     * Marks a penalty as paid.
     *
     * @param penaltyId the penalty ID
     * @param paymentId the payment ID
     * @return the updated penalty
     */
    @PostMapping("/{penaltyId}/pay")
    @Operation(summary = "Mark penalty as paid", description = "Marks a penalty as paid with a payment reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Penalty marked as paid"),
        @ApiResponse(responseCode = "400", description = "Penalty cannot be marked as paid"),
        @ApiResponse(responseCode = "404", description = "Penalty not found")
    })
    public ResponseEntity<?> markPenaltyAsPaid(
            @Parameter(description = "Penalty ID") @PathVariable UUID penaltyId,
            @Parameter(description = "Payment ID") @RequestParam UUID paymentId) {
        try {
            Penalty penalty = penaltyService.markPenaltyAsPaid(penaltyId, paymentId);
            return ResponseEntity.ok(penaltyMapper.toResponse(penalty));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Deletes a penalty (admin only).
     *
     * @param penaltyId the penalty ID
     * @return no content if successful
     */
    @DeleteMapping("/{penaltyId}")
    @Operation(summary = "Delete a penalty", description = "Deletes a penalty (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Penalty deleted"),
        @ApiResponse(responseCode = "404", description = "Penalty not found")
    })
    public ResponseEntity<Void> deletePenalty(
            @Parameter(description = "Penalty ID") @PathVariable UUID penaltyId) {
        try {
            penaltyService.deletePenalty(penaltyId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Penalty summary record.
     */
    private record PenaltySummary(UUID userId, long unpaidCount, BigDecimal totalUnpaidAmount, boolean hasUnpaidPenalties) {}

    /**
     * Simple error response class.
     */
    private record ErrorResponse(String message) {}
}

