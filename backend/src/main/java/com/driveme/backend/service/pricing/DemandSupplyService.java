package com.driveme.backend.service.pricing;

import com.driveme.backend.common.Location;
import com.driveme.backend.common.RequestStatus;
import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.config.PricingConfig;
import com.driveme.backend.repository.DriverRepository;
import com.driveme.backend.repository.TripRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for calculating demand/supply based surge pricing.
 * Compares active trip requests vs available drivers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemandSupplyService {

    private final PricingConfig config;
    private final TripRequestRepository tripRequestRepository;
    private final DriverRepository driverRepository;

    /**
     * Result record for surge calculation.
     */
    public record SurgeResult(
        double multiplier,
        int activeRequests,
        int availableDrivers,
        String level  // "LOW", "NORMAL", "MODERATE", "HIGH", "VERY_HIGH"
    ) {}

    /**
     * Calculate surge multiplier based on local demand/supply ratio.
     * 
     * @param location the pickup location
     * @return SurgeResult with multiplier and demand/supply info
     */
    public SurgeResult calculateSurgeMultiplier(Location location) {
        try {
            // Count active trip requests (pending, not yet matched)
            int activeRequests = countActiveRequests();
            
            // Count available drivers (verified and available)
            int availableDrivers = countAvailableDrivers();

            return calculateSurgeFromRatio(activeRequests, availableDrivers);
        } catch (Exception e) {
            log.warn("Error calculating surge, using default: {}", e.getMessage());
            return new SurgeResult(config.getMinSurgeMultiplier(), 0, 0, "NORMAL");
        }
    }

    /**
     * Count active (pending) trip requests.
     * In production, this would use geospatial queries for the specific area.
     */
    private int countActiveRequests() {
        try {
            return tripRequestRepository.findByStatus(RequestStatus.PENDING).size();
        } catch (Exception e) {
            log.debug("Could not count active requests: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Count available drivers.
     * In production, this would use geospatial queries for the specific area.
     */
    private int countAvailableDrivers() {
        try {
            // Count drivers who are verified and available
            return driverRepository.findByVerificationStatus(VerificationStatus.VERIFIED)
                    .stream()
                    .filter(d -> d.isAvailable())
                    .toList()
                    .size();
        } catch (Exception e) {
            log.debug("Could not count available drivers: {}", e.getMessage());
            return 10; // Default assumption to avoid division by zero
        }
    }

    /**
     * Calculate surge multiplier from request/driver ratio.
     */
    private SurgeResult calculateSurgeFromRatio(int activeRequests, int availableDrivers) {
        double minSurge = config.getMinSurgeMultiplier();

        // Avoid division by zero
        if (availableDrivers <= 0) {
            if (activeRequests > 0) {
                // High demand, no drivers - max surge
                double capped = Math.max(minSurge, config.getMaxSurgeMultiplier());
                return new SurgeResult(capped, activeRequests, 0, "VERY_HIGH");
            } else {
                // No requests, no drivers - normal
                return new SurgeResult(minSurge, 0, 0, "NORMAL");
            }
        }

        double ratio = (double) activeRequests / availableDrivers;
        
        double multiplier;
        String level;

        if (ratio <= 0.5) {
            // Low demand - could even offer discounts in the future
            multiplier = minSurge;
            level = "LOW";
        } else if (ratio <= 1.0) {
            // Balanced demand/supply
            multiplier = minSurge;
            level = "NORMAL";
        } else {
            // Demand exceeds supply - calculate surge
            double excessRatio = ratio - 1.0;
            int steps = (int) Math.ceil(excessRatio / config.getSurgeStepThreshold());
            multiplier = 1.0 + (steps * config.getSurgeStepIncrement());
            
            // Cap at max surge
            multiplier = Math.max(minSurge, Math.min(multiplier, config.getMaxSurgeMultiplier()));

            if (multiplier >= 2.0) {
                level = "VERY_HIGH";
            } else if (multiplier >= 1.5) {
                level = "HIGH";
            } else {
                level = "MODERATE";
            }
        }

        // Keep result inside configured surge boundaries.
        multiplier = Math.max(minSurge, Math.min(multiplier, config.getMaxSurgeMultiplier()));

        log.info("Surge: {} requests / {} drivers = ratio {}, multiplier {}, level {}",
                activeRequests, availableDrivers, ratio, multiplier, level);

        return new SurgeResult(multiplier, activeRequests, availableDrivers, level);
    }
}
