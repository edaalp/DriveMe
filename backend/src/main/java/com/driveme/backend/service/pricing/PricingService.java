package com.driveme.backend.service.pricing;

import com.driveme.backend.common.Location;
import com.driveme.backend.config.PricingConfig;
import com.driveme.backend.dto.PricingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Main pricing orchestration service.
 * 
 * === ANKARA TAXI TARIFF (MARCH 2026) ===
 * - Bindi-İndi (minimum fare): 200 TL
 * - Açılış (opening fee): 65 TL
 * - Kilometre: 40 TL/km
 * - Saat (hourly): 420 TL/hour
 * - Dakika (per minute): 7 TL/min
 * - 100 metre: 4 TL
 * 
 * === PRICING FORMULA ===
 * 1. Forward Trip Cost = Opening Fee + (Distance × Per-km) + (Duration × Per-min)
 * 2. Driver Return Compensation = Forward Cost × Return Factor (default 0.7)
 * 3. Subtotal = Forward + Return Compensation
 * 4. Multiplied Cost = Subtotal × Time × Surge × Weather × Traffic multipliers
 * 5. Final Price = Multiplied Cost × (1 + Service Fee %)
 * 6. Apply min/max range for negotiation flexibility
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private static final String[] POPULAR_DESTINATION_KEYWORDS = {
            "airport", "esenboga", "otogar", "terminal", "avm", "mall", "gar", "station"
    };

    private final RoutingService routingService;
    private final TimePricingService timePricingService;
    private final DemandSupplyService demandSupplyService;
    private final WeatherService weatherService;
    private final TrafficService trafficService;
    private final PricingConfig config;

    /**
     * Calculate price using current time.
     */
    public PricingResult calculatePrice(Location pickup, Location destination) {
        return calculatePrice(pickup, destination, LocalDateTime.now(ZoneId.of("Europe/Istanbul")));
    }

    /**
     * Calculate price for a specific time (useful for scheduled trips).
     * 
     * @param pickup starting location
     * @param destination ending location
     * @param requestTime the time when the trip will occur
     * @return PricingResult with all pricing details
     */
    public PricingResult calculatePrice(Location pickup, Location destination, LocalDateTime requestTime) {
        log.info("=== PRICING CALCULATION START ===");
        log.info("From: ({}, {}) To: ({}, {})",
                pickup.getLat(), pickup.getLon(), destination.getLat(), destination.getLon());

        // ===== STEP 1: Get Route Information =====
        RoutingService.RoutingResult route = routingService.calculateRoute(pickup, destination);
        log.info("Route: {} km, {} minutes", round(route.distanceKm), route.durationMinutes);

        // ===== STEP 2: Calculate Base Taxi Cost (Forward Trip) =====
        double forwardTaxiCost = calculateTaxiMeterCost(route.distanceKm, route.durationMinutes);
        log.info("Forward taxi cost: {} TL", round(forwardTaxiCost));

        // ===== STEP 3: Calculate Driver Return Compensation =====
        // Driver needs compensation for going back or finding another ride
        // Using configurable factor (default 70% of forward trip)
        boolean popularDestinationApplied = isPopularDestination(destination);
        double returnFactor = popularDestinationApplied
                ? config.getPopularDestinationReturnFactor()
                : config.getDriverReturnFactor();
        double returnCompensation = forwardTaxiCost * returnFactor;
        log.info("Return compensation ({}%): {} TL",
                round(returnFactor * 100), round(returnCompensation));

        double baseCost = forwardTaxiCost + returnCompensation;
        log.info("Base cost (forward + return): {} TL", round(baseCost));

        // ===== STEP 4: Calculate All Multipliers =====
        // Time multiplier (rush hour, night, weekend)
        TimePricingService.TimeMultiplierResult timeResult = 
            timePricingService.calculateTimeMultiplier(requestTime);
        log.info("Time multiplier: {} ({})", round(timeResult.multiplier()), timeResult.reason());

        // Demand/supply surge multiplier
        DemandSupplyService.SurgeResult surgeResult = 
            demandSupplyService.calculateSurgeMultiplier(pickup);
        log.info("Surge multiplier: {} ({} requests, {} drivers, level: {})",
                round(surgeResult.multiplier()), surgeResult.activeRequests(),
                surgeResult.availableDrivers(), surgeResult.level());

        // Weather multiplier
        double weatherMultiplier = weatherService.calculateWeatherMultiplier(pickup);
        log.info("Weather multiplier: {}", round(weatherMultiplier));

        // Traffic multiplier
        TrafficService.TrafficResult trafficResult = 
            trafficService.calculateTrafficMultiplier(pickup, destination);
        log.info("Traffic multiplier: {} (level: {})",
                round(trafficResult.multiplier()), trafficResult.trafficLevel());

        // ===== STEP 5: Apply All Multipliers =====
        double combinedMultiplier = timeResult.multiplier() 
                                  * surgeResult.multiplier() 
                                  * weatherMultiplier 
                                  * trafficResult.multiplier();

        // Cap combined multiplier to prevent extreme prices
        double maxCombinedMultiplier = config.getMaxSurgeMultiplier() * 1.5;
        if (combinedMultiplier > maxCombinedMultiplier) {
            log.warn("Combined multiplier {} exceeds max {}, capping",
                    round(combinedMultiplier), round(maxCombinedMultiplier));
            combinedMultiplier = maxCombinedMultiplier;
        }

        double adjustedCost = baseCost * combinedMultiplier;
        log.info("After multipliers ({}x): {} TL", round(combinedMultiplier), round(adjustedCost));

        // ===== STEP 6: Add Service Fee =====
        double serviceFee = adjustedCost * config.getServicePercentage();
        double finalPrice = adjustedCost + serviceFee;
        log.info("Service fee ({}%): {} TL", round(config.getServicePercentage() * 100), round(serviceFee));
        log.info("Final price: {} TL", round(finalPrice));

        // ===== STEP 7: Calculate Min/Max Range =====
        double minPrice = finalPrice * config.getMinPricePercentage();
        double maxPrice = finalPrice * config.getMaxPricePercentage();

        // Ensure minimum fare is respected
        double effectiveMinFare = config.getTaxiMinFare() * (1 + config.getServicePercentage());
        if (minPrice < effectiveMinFare) {
            log.info("Min price {} below minimum fare {}, adjusting", round(minPrice), round(effectiveMinFare));
            minPrice = effectiveMinFare;
            maxPrice = Math.max(maxPrice, minPrice * 1.1);
        }

        log.info("Price range: {} - {} TL", round(minPrice), round(maxPrice));
        log.info("=== PRICING CALCULATION END ===");

        return PricingResult.builder()
                .distanceKm(route.distanceKm)
                .durationMinutes(route.durationMinutes)
                .basePrice(round(baseCost))
                .surgeMultiplier(round(surgeResult.multiplier()))
                .timeMultiplier(round(timeResult.multiplier()))
                .weatherMultiplier(round(weatherMultiplier))
                .trafficMultiplier(round(trafficResult.multiplier()))
                .combinedMultiplier(round(combinedMultiplier))
                .returnFactor(round(returnFactor))
                .serviceFee(round(serviceFee))
                .popularDestinationApplied(popularDestinationApplied)
                .finalBasePrice(round(finalPrice))
                .minPrice(round(minPrice))
                .maxPrice(round(maxPrice))
                .currency(config.getCurrency())
                .build();
    }

    /**
     * Calculate taxi meter cost using official Ankara taxi tariff (March 2026).
     * 
     * Formula: Opening Fee + (Distance × Per-km Rate) + (Duration × Per-minute Rate)
     * Result is max of calculated cost or minimum fare (bindi-indi)
     * 
     * @param distanceKm distance in kilometers
     * @param durationMinutes duration in minutes
     * @return taxi meter cost in TL
     */
    private double calculateTaxiMeterCost(double distanceKm, int durationMinutes) {
        // Opening fee: 65 TL
        double cost = config.getTaxiOpeningFee();
        
        // Distance cost: prefer per-100m tariff when available for better precision.
        double distanceCost;
        if (config.getTaxiPer100mRate() > 0) {
            distanceCost = distanceKm * 10.0 * config.getTaxiPer100mRate();
        } else {
            distanceCost = distanceKm * config.getTaxiPerKmRate();
        }
        cost += distanceCost;

        // Time cost: 7 TL/minute (for waiting/slow traffic time)
        // Note: In a real taxi, per-minute is only charged during stops/slow movement
        // For simplicity, we use a fraction of duration
        // Assuming ~30% of trip time involves waiting/slow movement
        double effectiveWaitMinutes = durationMinutes * 0.3;
        double perMinuteRate = config.getTaxiPerMinuteRate();
        if (config.getTaxiHourlyRate() > 0) {
            perMinuteRate = Math.max(perMinuteRate, config.getTaxiHourlyRate() / 60.0);
        }
        cost += effectiveWaitMinutes * perMinuteRate;

        // Apply minimum fare (bindi-indi): 200 TL
        return Math.max(cost, config.getTaxiMinFare());
    }

    private boolean isPopularDestination(Location destination) {
        if (destination == null || destination.getAddressText() == null) {
            return false;
        }
        String text = destination.getAddressText().toLowerCase(Locale.ROOT);
        for (String keyword : POPULAR_DESTINATION_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Round to 2 decimal places.
     */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}