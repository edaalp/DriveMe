package com.driveme.backend.controller;

import com.driveme.backend.common.Location;
import com.driveme.backend.dto.CalculatePriceRequest;
import com.driveme.backend.dto.ErrorResponse;
import com.driveme.backend.dto.PricingResult;
import com.driveme.backend.dto.TripPricePreview;
import com.driveme.backend.helper.TripRequestMapper;
import com.driveme.backend.service.pricing.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for trip pricing operations.
 * Provides endpoints for price calculation and estimation.
 * Both endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/trip-requests")
@RequiredArgsConstructor
@Slf4j
public class TripPricingController {

    private final PricingService pricingService;

    /**
     * Calculate price preview for a trip.
     * This endpoint is called by the Flutter app to show the user an estimated price range
     * before they create the actual trip request.
     *
     * HTTP: POST /api/trip-requests/calculate-price
     * Auth: Required (Bearer JWT token)
     *
     * Request body:
     * {
     *   "pickup": { "lat": <double>, "lon": <double>, "addressText": "<string>" },
     *   "destination": { "lat": <double>, "lon": <double>, "addressText": "<string>" }
     * }
     *
     * Response body (on success):
     * {
     *   "minPrice": <double>,
     *   "maxPrice": <double>,
     *   "currency": "TRY",
     *   "distanceKm": <double>,
     *   "durationMinutes": <int> (optional)
     * }
     *
     * Response body (on error):
     * {
     *   "message": "<error description>",
     *   "errorId": "<optional error id>",
     *   "timestamp": <milliseconds>
     * }
     *
     * @param authentication the authenticated user (passenger)
     * @param request the price calculation request with pickup and destination
     * @return TripPricePreview with minPrice, maxPrice, currency, and distanceKm
     */
    @PostMapping("/calculate-price")
    public ResponseEntity<?> calculatePrice(
            Authentication authentication,
            @Valid @RequestBody CalculatePriceRequest request) {

        try {
            log.info("Calculating price from {} to {}", request.getPickup(), request.getDestination());

            // Convert DTOs to Location objects
            Location pickup = TripRequestMapper.toLocation(request.getPickup());
            Location destination = TripRequestMapper.toLocation(request.getDestination());

            // Call the pricing service (which does all the heavy lifting)
            PricingResult pricingResult = pricingService.calculatePrice(
                    pickup,
                    destination,
                    null,  // vehicle - not relevant for preview
                    false, // withPet - not relevant for preview
                    null,  // requestedTime - use current time
                    null   // passenger - not relevant for preview
            );

            // Convert to Flutter's expected response format
            TripPricePreview preview = TripPricePreview.builder()
                    .minPrice(pricingResult.getMinPrice())
                    .maxPrice(pricingResult.getMaxPrice())
                    .currency(pricingResult.getCurrency())
                    .distanceKm(pricingResult.getDistanceKm())
                    .durationMinutes(pricingResult.getDurationMinutes())
                    .build();

            log.info("Price preview calculated: {} - {}", pricingResult.getMinPrice(), pricingResult.getMaxPrice());
            return ResponseEntity.ok(preview);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error in price calculation: {}", e.getMessage());
            ErrorResponse error = ErrorResponse.builder()
                    .message(e.getMessage())
                    .errorId("VALIDATION_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (RuntimeException e) {
            log.error("Error calculating price", e);
            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to calculate price: " + e.getMessage())
                    .errorId("PRICING_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);

        } catch (Exception e) {
            log.error("Unexpected error in price calculation", e);
            ErrorResponse error = ErrorResponse.builder()
                    .message("Internal server error")
                    .errorId("INTERNAL_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

