package com.driveme.backend.service.pricing;

import com.driveme.backend.common.Location;
import com.driveme.backend.config.PricingConfig;
import com.driveme.backend.dto.PricingResult;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Central pricing service that implements the complete pricing formula.
 * This is the single source of truth for all price calculations in the system.
 *
 * Pricing formula:
 * 1. Calculate base price: baseFare + (distanceKm * perKmRate) + (durationMin * perMinuteRate)
 * 2. Apply surge multiplier based on demand/supply
 * 3. Apply time multiplier (rush hour, night, etc.)
 * 4. Apply weather multiplier
 * 5. Create price range: minPrice = finalBase * 0.9, maxPrice = finalBase * 1.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final PricingConfig pricingConfig;
    private final RoutingService routingService;
    private final DemandSupplyService demandSupplyService;
    private final TimePricingService timePricingService;
    private final WeatherPricingService weatherPricingService;

    /**
     * Calculate price for a trip with all factors considered.
     * This method is the single entry point for all pricing calculations.
     *
     * @param pickup starting location
     * @param destination ending location
     * @param vehicle the vehicle (can be null for pricing estimates)
     * @param withPet whether the trip includes a pet
     * @param requestedTime when the trip is requested for
     * @param passenger the passenger (for future use, e.g., loyalty discounts)
     * @return PricingResult with complete pricing breakdown
     */
    public PricingResult calculatePrice(
            Location pickup,
            Location destination,
            Vehicle vehicle,
            boolean withPet,
            Instant requestedTime,
            Passenger passenger) {

        log.info("Calculating price for trip from {} to {}", pickup, destination);

        try {
            // Step 1: Get distance and duration from routing service
            RoutingService.RoutingResult routing = routingService.calculateRoute(pickup, destination);
            double distanceKm = routing.distanceKm;
            int durationMinutes = routing.durationMinutes;

            log.debug("Route calculated: {} km, {} minutes", distanceKm, durationMinutes);

            // Step 2: Calculate base price (distance + time)
            double basePrice = calculateBasePrice(distanceKm, durationMinutes);
            log.debug("Base price: {}", basePrice);

            // Step 3: Calculate all multipliers
            double surgeMultiplier = demandSupplyService.calculateSurgeMultiplier(pickup);
            double timeMultiplier = timePricingService.calculateTimeMultiplier(requestedTime);
            double weatherMultiplier = weatherPricingService.calculateWeatherMultiplier(pickup, requestedTime);

            log.debug("Multipliers - Surge: {}, Time: {}, Weather: {}", surgeMultiplier, timeMultiplier, weatherMultiplier);

            // Step 4: Apply all multipliers to base price
            double finalBasePrice = basePrice * surgeMultiplier * timeMultiplier * weatherMultiplier;
            log.debug("Final base price after multipliers: {}", finalBasePrice);

            // Step 5: Calculate min and max prices (with 10% range)
            double minPrice = finalBasePrice * pricingConfig.getMinPricePercentage();
            double maxPrice = finalBasePrice * pricingConfig.getMaxPricePercentage();

            // Round to 2 decimal places
            minPrice = roundPrice(minPrice);
            maxPrice = roundPrice(maxPrice);

            log.info("Final price range: {} - {}", minPrice, maxPrice);

            // Build result
            return PricingResult.builder()
                    .distanceKm(distanceKm)
                    .durationMinutes(durationMinutes)
                    .basePrice(basePrice)
                    .surgeMultiplier(surgeMultiplier)
                    .timeMultiplier(timeMultiplier)
                    .weatherMultiplier(weatherMultiplier)
                    .finalBasePrice(finalBasePrice)
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .currency(pricingConfig.getCurrency())
                    .build();

        } catch (Exception e) {
            log.error("Error calculating price", e);
            throw new RuntimeException("Failed to calculate trip price: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate base price based on distance and duration.
     *
     * @param distanceKm distance in kilometers
     * @param durationMinutes estimated duration in minutes
     * @return base price
     */
    private double calculateBasePrice(double distanceKm, int durationMinutes) {
        double baseFare = pricingConfig.getBaseFare();
        double perKmRate = pricingConfig.getPerKmRate();
        double perMinuteRate = pricingConfig.getPerMinuteRate();

        double price = baseFare + (distanceKm * perKmRate) + (durationMinutes * perMinuteRate);
        log.debug("Base price calculation: baseFare={} + ({}km * {}/km) + ({}min * {}/min) = {}",
                baseFare, distanceKm, perKmRate, durationMinutes, perMinuteRate, price);
        return price;
    }

    /**
     * Round price to 2 decimal places using HALF_UP strategy.
     *
     * @param price the price to round
     * @return rounded price
     */
    private double roundPrice(double price) {
        BigDecimal bd = BigDecimal.valueOf(price);
        return bd.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

