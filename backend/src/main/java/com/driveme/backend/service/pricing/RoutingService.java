package com.driveme.backend.service.pricing;

import com.driveme.backend.common.Location;
import com.driveme.backend.config.PricingConfig;
import com.google.maps.DirectionsApi;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for calculating distance and duration using routing APIs.
 * Supports Google Maps API with traffic data, falls back to Haversine formula.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingService {

    private final PricingConfig pricingConfig;

    /**
     * DTO for routing result.
     */
    public static class RoutingResult {
        public double distanceKm;
        public int durationMinutes;
        public int durationInTrafficMinutes;  // -1 if not available
        public String source;  // "GOOGLE_MAPS" or "HAVERSINE"

        public RoutingResult(double distanceKm, int durationMinutes) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.durationInTrafficMinutes = -1;
            this.source = "HAVERSINE";
        }

        public RoutingResult(double distanceKm, int durationMinutes, int durationInTrafficMinutes, String source) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.durationInTrafficMinutes = durationInTrafficMinutes;
            this.source = source;
        }
    }

    /**
     * Calculate distance and duration between two locations.
     * Uses Google Maps API if configured, otherwise falls back to Haversine formula.
     */
    public RoutingResult calculateRoute(Location pickup, Location destination) {
        if (pickup == null || destination == null) {
            throw new IllegalArgumentException("Pickup and destination locations are required");
        }

        // Validate coordinates
        if (!isValidCoordinate(pickup.getLat(), pickup.getLon()) ||
                !isValidCoordinate(destination.getLat(), destination.getLon())) {
            throw new IllegalArgumentException("Invalid coordinates provided");
        }

        // Try Google Maps API if enabled
        if (pricingConfig.isUseGoogleMaps() && isApiKeyValid(pricingConfig.getGoogleMapsApiKey())) {
            try {
                return calculateRouteWithGoogleMaps(pickup, destination);
            } catch (Exception e) {
                log.warn("Google Maps API failed, falling back to Haversine: {}", e.getMessage());
            }
        } else {
            log.debug("Google Maps disabled or no API key, using Haversine formula");
        }

        // Fallback to Haversine
        return calculateRouteWithHaversine(pickup, destination);
    }

    /**
     * Calculate route using Google Maps Distance Matrix API.
     * If traffic data is enabled, also fetches duration_in_traffic.
     */
    private RoutingResult calculateRouteWithGoogleMaps(Location pickup, Location destination) throws Exception {
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(pricingConfig.getGoogleMapsApiKey())
                .build();

        try {
            LatLng pickupLatLng = new LatLng(pickup.getLat(), pickup.getLon());
            LatLng destinationLatLng = new LatLng(destination.getLat(), destination.getLon());

            DistanceMatrix result;

            if (pricingConfig.isUseTrafficData()) {
                // Request with departure_time=now to get traffic data
                result = DistanceMatrixApi.newRequest(context)
                        .origins(pickupLatLng)
                        .destinations(destinationLatLng)
                        .mode(TravelMode.DRIVING)
                        .departureTime(Instant.now())
                        .trafficModel(TrafficModel.BEST_GUESS)
                        .await();
            } else {
                // Standard request without traffic
                result = DistanceMatrixApi.newRequest(context)
                        .origins(pickupLatLng)
                        .destinations(destinationLatLng)
                        .mode(TravelMode.DRIVING)
                        .await();
            }

            // Validate response
            if (result.rows == null || result.rows.length == 0 ||
                    result.rows[0].elements == null || result.rows[0].elements.length == 0) {
                throw new RuntimeException("Empty response from Google Maps API");
            }

            DistanceMatrixElement element = result.rows[0].elements[0];

            // Check for errors in the response
            if (element.status != DistanceMatrixElementStatus.OK) {
                throw new RuntimeException("Google Maps API error: " + element.status);
            }

            if (element.distance == null || element.duration == null) {
                throw new RuntimeException("No distance/duration in response");
            }

            double distanceKm = element.distance.inMeters / 1000.0;
            int durationMinutes = (int) Math.ceil(element.duration.inSeconds / 60.0);

            // Get traffic duration if available
            int durationInTrafficMinutes = -1;
            if (element.durationInTraffic != null) {
                durationInTrafficMinutes = (int) Math.ceil(element.durationInTraffic.inSeconds / 60.0);
                log.info("Google Maps: {:.2f} km, {} min (in traffic: {} min)",
                        distanceKm, durationMinutes, durationInTrafficMinutes);
            } else {
                log.info("Google Maps: {:.2f} km, {} min", distanceKm, durationMinutes);
            }

            return new RoutingResult(distanceKm, durationMinutes, durationInTrafficMinutes, "GOOGLE_MAPS");

        } finally {
            context.shutdown();
        }
    }

    /**
     * Calculate route using Haversine formula (straight-line distance).
     * This is a fallback when Google Maps is not available.
     */
    private RoutingResult calculateRouteWithHaversine(Location pickup, Location destination) {
        double distanceKm = calculateHaversineDistance(
                pickup.getLat(), pickup.getLon(),
                destination.getLat(), destination.getLon()
        );

        // Multiply by road factor (roads are typically 1.3x straight-line distance)
        double roadDistance = distanceKm * 1.3;

        // Estimate duration: assume 35 km/h average in Ankara (traffic considered)
        double averageSpeedKmh = 35.0;
        int durationMinutes = (int) Math.ceil((roadDistance / averageSpeedKmh) * 60);

        // Minimum duration of 5 minutes
        durationMinutes = Math.max(durationMinutes, 5);

        log.info("Haversine: straight-line {:.2f} km, estimated road {:.2f} km, {} min",
                distanceKm, roadDistance, durationMinutes);

        return new RoutingResult(roadDistance, durationMinutes, -1, "HAVERSINE");
    }

    /**
     * Calculate straight-line distance using Haversine formula.
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth's radius in km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Validate coordinate values.
     */
    private boolean isValidCoordinate(double lat, double lon) {
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    /**
     * Check if API key is valid (not null/empty).
     */
    private boolean isApiKeyValid(String apiKey) {
        return apiKey != null && !apiKey.isBlank();
    }
}