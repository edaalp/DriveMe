package com.driveme.backend.controller;

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

@RestController
@RequestMapping("/api/trip-requests")
@RequiredArgsConstructor
@Slf4j
public class TripPricingController {

    private final PricingService pricingService;

    @PostMapping("/calculate-price")
    public ResponseEntity<?> calculatePrice(
            Authentication authentication,
            @Valid @RequestBody CalculatePriceRequest request) {

        try {
            log.info("Calculating price from {} to {}", request.getPickup(), request.getDestination());

            PricingResult pricingResult = pricingService.calculatePrice(
                    TripRequestMapper.toLocation(request.getPickup()),
                    TripRequestMapper.toLocation(request.getDestination())
            );

            TripPricePreview preview = TripPricePreview.builder()
                    .minPrice(pricingResult.getMinPrice())
                    .maxPrice(pricingResult.getMaxPrice())
                    .currency(pricingResult.getCurrency())
                    .distanceKm(pricingResult.getDistanceKm())
                    .durationMinutes(pricingResult.getDurationMinutes())
                    .timeMultiplier(pricingResult.getTimeMultiplier())
                    .surgeMultiplier(pricingResult.getSurgeMultiplier())
                    .weatherMultiplier(pricingResult.getWeatherMultiplier())
                    .trafficMultiplier(pricingResult.getTrafficMultiplier())
                    .combinedMultiplier(pricingResult.getCombinedMultiplier())
                    .serviceFee(pricingResult.getServiceFee())
                    .returnFactor(pricingResult.getReturnFactor())
                    .popularDestinationApplied(pricingResult.isPopularDestinationApplied())
                    .build();

            log.info("Price preview calculated: {} - {} TRY",
                    pricingResult.getMinPrice(), pricingResult.getMaxPrice());
            return ResponseEntity.ok(preview);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error in price calculation: {}", e.getMessage());
            ErrorResponse error = ErrorResponse.builder()
                    .message(e.getMessage())
                    .errorId("VALIDATION_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            log.error("Unexpected error in price calculation", e);
            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to calculate price: " + e.getMessage())
                    .errorId("PRICING_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}