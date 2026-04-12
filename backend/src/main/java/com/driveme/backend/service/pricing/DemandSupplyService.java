package com.driveme.backend.service.pricing;
import com.driveme.backend.common.Location;
import com.driveme.backend.repository.TripRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
/**
 * Service for calculating demand-supply based surge pricing.
 * Currently provides a stub implementation that returns 1.0 (no surge).
 * In the future, this can be extended to use real demand/supply data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemandSupplyService {
    private final TripRequestRepository tripRequestRepository;
    /**
     * Calculate surge multiplier based on demand and supply in an area.
     * Formula: surge = min(3.0, 1.0 + 0.5 * log(demand / max(supply, 1)))
     *
     * @param location the area to calculate surge for
     * @return surge multiplier (1.0 = no surge, > 1.0 = surge pricing)
     */
    public double calculateSurgeMultiplier(Location location) {
        try {
            int demand = getActiveDemandInArea(location);
            int supply = getAvailableSupplyInArea(location);
            double demandSupplyRatio = (double) demand / Math.max(supply, 1);
            double surge = Math.min(3.0, 1.0 + 0.5 * Math.log(demandSupplyRatio));
            log.debug("Surge calculation for area {}: demand={}, supply={}, ratio={}, surge={}",
                    location, demand, supply, demandSupplyRatio, surge);
            return surge;
        } catch (Exception e) {
            log.warn("Error calculating surge multiplier, returning 1.0", e);
            return 1.0;
        }
    }
    private int getActiveDemandInArea(Location location) {
        return 1;
    }
    private int getAvailableSupplyInArea(Location location) {
        return 1;
    }
}