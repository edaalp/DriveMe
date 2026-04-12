package com.driveme.backend.service.pricing;
import com.driveme.backend.common.Location;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
/**
 * Service for calculating weather-based pricing multipliers.
 * Stub implementation that always returns 1.0.
 * Can be extended to integrate with weather APIs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherPricingService {
    public double calculateWeatherMultiplier(Location location, Instant time) {
        return 1.0;
    }
}