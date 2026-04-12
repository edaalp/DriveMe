package com.driveme.backend.service.pricing;

import com.driveme.backend.common.Location;
import com.driveme.backend.config.PricingConfig;
import com.driveme.backend.dto.PricingResult;
import com.driveme.backend.service.pricing.RoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final RoutingService routingService;
    private final PricingConfig config;

    public PricingResult calculatePrice(Location pickup, Location destination) {
        RoutingService.RoutingResult route = routingService.calculateRoute(pickup, destination);

        double forwardTaxiCost = taxiMeterCost(route.distanceKm, route.durationMinutes);
        double returnTaxiCost  = taxiMeterCost(route.distanceKm, route.durationMinutes);

        double totalTaxiCost = forwardTaxiCost + returnTaxiCost;
        double serviceFee    = totalTaxiCost * config.getServicePercentage();
        double finalPrice    = totalTaxiCost + serviceFee;

        return PricingResult.builder()
                .minPrice(finalPrice * config.getMinPricePercentage())
                .maxPrice(finalPrice * config.getMaxPricePercentage())
                .currency(config.getCurrency())
                .distanceKm(route.distanceKm)
                .durationMinutes(route.durationMinutes)
                .build();
    }

    private double taxiMeterCost(double distanceKm, int durationMinutes) {
        double cost = config.getTaxiOpeningFee()
                + (distanceKm * config.getTaxiPerKmRate())
                + (durationMinutes * config.getTaxiPerMinuteRate());
        return Math.max(cost, config.getTaxiMinFare());
    }
}