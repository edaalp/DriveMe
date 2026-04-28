package com.driveme.backend.service.pricing;

import com.driveme.backend.common.Location;
import com.driveme.backend.config.PricingConfig;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for calculating traffic-based pricing multipliers.
 * Uses Google Maps Directions API to compare normal vs traffic-aware duration.
 *
 * Requires:
 * - Google Maps API key with Directions API enabled
 * - useTrafficData = true in config
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficService {

    private final PricingConfig config;

    /**
     * Traffic result record.
     */
    public record TrafficResult(
            double multiplier,
            int normalDurationMinutes,
            int trafficDurationMinutes,
            String trafficLevel  // "LIGHT", "MODERATE", "HEAVY", "SEVERE", "UNKNOWN"
    ) {}

    /**
     * Calculate traffic multiplier by comparing normal vs in-traffic duration.
     * Returns 1.0 if traffic data is disabled or unavailable.
     */
    public TrafficResult calculateTrafficMultiplier(Location pickup, Location destination) {
        // Check if traffic data is enabled and API key exists
        if (!config.isUseTrafficData()) {
            log.debug("Traffic data disabled, returning default multiplier 1.0");
            return new TrafficResult(1.0, 0, 0, "DISABLED");
        }

        if (!config.isUseGoogleMaps() || !isApiKeyValid(config.getGoogleMapsApiKey())) {
            log.debug("Google Maps not configured, returning default multiplier 1.0");
            return new TrafficResult(1.0, 0, 0, "NO_API");
        }

        try {
            return fetchTrafficData(pickup, destination);
        } catch (Exception e) {
            log.warn("Failed to fetch traffic data: {}", e.getMessage());
            return new TrafficResult(1.0, 0, 0, "ERROR");
        }
    }

    /**
     * Fetch traffic data from Google Maps Directions API.
     */
    private TrafficResult fetchTrafficData(Location pickup, Location destination) throws Exception {
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(config.getGoogleMapsApiKey())
                .build();

        try {
            LatLng origin = new LatLng(pickup.getLat(), pickup.getLon());
            LatLng dest = new LatLng(destination.getLat(), destination.getLon());

            log.debug("Fetching traffic data from ({:.4f}, {:.4f}) to ({:.4f}, {:.4f})",
                    pickup.getLat(), pickup.getLon(), destination.getLat(), destination.getLon());

            // Request directions with departure time = now for real-time traffic
            DirectionsResult result = DirectionsApi.newRequest(context)
                    .origin(origin)
                    .destination(dest)
                    .mode(TravelMode.DRIVING)
                    .departureTime(Instant.now())
                    .trafficModel(TrafficModel.BEST_GUESS)
                    .await();

            // Validate response
            if (result.routes == null || result.routes.length == 0) {
                throw new RuntimeException("No routes found in Directions API response");
            }

            DirectionsRoute route = result.routes[0];
            if (route.legs == null || route.legs.length == 0) {
                throw new RuntimeException("No legs found in route");
            }

            DirectionsLeg leg = route.legs[0];

            // Normal duration (without traffic consideration)
            int normalDurationMinutes = 0;
            if (leg.duration != null) {
                normalDurationMinutes = (int) Math.ceil(leg.duration.inSeconds / 60.0);
            }

            // Duration in traffic (real-time)
            int trafficDurationMinutes = normalDurationMinutes;
            if (leg.durationInTraffic != null) {
                trafficDurationMinutes = (int) Math.ceil(leg.durationInTraffic.inSeconds / 60.0);
            }

            // Calculate traffic ratio and determine multiplier
            return calculateTrafficMultiplier(normalDurationMinutes, trafficDurationMinutes);

        } finally {
            context.shutdown();
        }
    }

    /**
     * Calculate traffic multiplier from duration comparison.
     */
    private TrafficResult calculateTrafficMultiplier(int normalMinutes, int trafficMinutes) {
        // Avoid division by zero
        if (normalMinutes <= 0) {
            return new TrafficResult(1.0, normalMinutes, trafficMinutes, "UNKNOWN");
        }

        double ratio = (double) trafficMinutes / normalMinutes;

        double multiplier;
        String trafficLevel;

        if (ratio <= 1.1) {
            // Traffic adds ≤10% - considered light
            multiplier = 1.0;
            trafficLevel = "LIGHT";
        } else if (ratio <= 1.3) {
            // Traffic adds 10-30% - moderate
            multiplier = config.getModerateTrafficMultiplier();
            trafficLevel = "MODERATE";
        } else if (ratio <= 1.6) {
            // Traffic adds 30-60% - heavy
            multiplier = config.getHeavyTrafficMultiplier();
            trafficLevel = "HEAVY";
        } else {
            // Traffic adds >60% - severe
            multiplier = config.getSevereTrafficMultiplier();
            trafficLevel = "SEVERE";
        }

        log.info("Traffic analysis: normal {} min, in-traffic {} min, ratio {:.2f}, level {}, multiplier {:.2f}",
                normalMinutes, trafficMinutes, ratio, trafficLevel, multiplier);

        return new TrafficResult(multiplier, normalMinutes, trafficMinutes, trafficLevel);
    }

    /**
     * Check if API key is valid.
     */
    private boolean isApiKeyValid(String apiKey) {
        return apiKey != null && !apiKey.isBlank() && apiKey.length() > 10;
    }
}