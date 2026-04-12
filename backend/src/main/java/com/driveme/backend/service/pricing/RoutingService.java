package com.driveme.backend.service.pricing;

import com.driveme.backend.common.Location;
import com.driveme.backend.config.PricingConfig;
import com.google.maps.GeoApiContext;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for calculating distance and duration using routing APIs.
 * Currently uses Google Maps Directions API, but can be extended to support other providers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingService {

    private final PricingConfig pricingConfig;

    /**
     * DTO for routing result
     */
    public static class RoutingResult {
        public double distanceKm;
        public int durationMinutes;

        public RoutingResult(double distanceKm, int durationMinutes) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
        }
    }

    /**
     * Calculate distance and duration between two locations.
     * Uses Google Maps API if configured, otherwise falls back to Haversine formula.
     *
     * @param pickup starting location
     * @param destination ending location
     * @return RoutingResult with distance in km and duration in minutes
     */
    public RoutingResult calculateRoute(Location pickup, Location destination) {
        if (pickup == null || destination == null) {
            throw new IllegalArgumentException("Pickup and destination locations are required");
        }

        try {
            if (pricingConfig.isUseGoogleMaps() && pricingConfig.getGoogleMapsApiKey() != null) {
                return calculateRouteWithGoogleMaps(pickup, destination);
            } else {
                return calculateRouteWithHaversine(pickup, destination);
            }
        } catch (Exception e) {
            log.error("Error calculating route, falling back to Haversine formula", e);
            return calculateRouteWithHaversine(pickup, destination);
        }
    }

    /**
     * Calculate route using Google Maps Distance Matrix API.
     *
     * @param pickup starting location
     * @param destination ending location
     * @return RoutingResult with distance in km and duration in minutes
     */
    private RoutingResult calculateRouteWithGoogleMaps(Location pickup, Location destination) {
        try {
            GeoApiContext context = new GeoApiContext.Builder()
                    .apiKey(pricingConfig.getGoogleMapsApiKey())
                    .build();

            LatLng pickupLatLng = new LatLng(pickup.getLat(), pickup.getLon());
            LatLng destinationLatLng = new LatLng(destination.getLat(), destination.getLon());

            DistanceMatrix result = DistanceMatrixApi.newRequest(context)
                    .origins(pickupLatLng)
                    .destinations(destinationLatLng)
                    .await();

            if (result.rows.length > 0 && result.rows[0].elements.length > 0) {
                long distanceInMeters = result.rows[0].elements[0].distance.inMeters;
                long durationInSeconds = result.rows[0].elements[0].duration.inSeconds;

                double distanceKm = distanceInMeters / 1000.0;
                int durationMinutes = (int) Math.ceil(durationInSeconds / 60.0);

                log.info("Google Maps routing: {} km in {} minutes", distanceKm, durationMinutes);
                return new RoutingResult(distanceKm, durationMinutes);
            } else {
                throw new RuntimeException("No route found between pickup and destination");
            }
        } catch (Exception e) {
            log.error("Google Maps API call failed", e);
            throw new RuntimeException("Failed to calculate route: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate route using Haversine formula (fallback method).
     * Uses straight-line distance approximation, which is less accurate but doesn't require API calls.
     *
     * @param pickup starting location
     * @param destination ending location
     * @return RoutingResult with estimated distance in km and duration in minutes
     */
    private RoutingResult calculateRouteWithHaversine(Location pickup, Location destination) {
        double distanceKm = pickup.distanceTo(destination);

        // Estimate duration: assume average speed of 40 km/h in urban areas
        // This is a rough estimate; in production, you'd want more sophisticated logic
        double averageSpeedKmh = 40.0;
        int durationMinutes = (int) Math.ceil((distanceKm / averageSpeedKmh) * 60);

        log.info("Haversine calculation: {} km, estimated {} minutes", distanceKm, durationMinutes);
        return new RoutingResult(distanceKm, durationMinutes);
    }
}

