package com.driveme.backend.service.pricing;

import com.driveme.backend.common.Location;
import com.driveme.backend.config.PricingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for fetching weather data and calculating weather-based multipliers.
 * Uses OpenWeatherMap API (free tier: 1000 calls/day).
 *
 * API Documentation: https://openweathermap.org/current
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final PricingConfig config;
    private final RestTemplate restTemplate;

    // Simple cache to avoid excessive API calls (cache for 10 minutes)
    private final ConcurrentHashMap<String, CachedWeather> weatherCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    /**
     * Weather data record.
     */
    public record WeatherData(
            String condition,      // Main condition: "Rain", "Snow", "Clear", etc.
            String description,    // Detailed description
            double temperature,    // Temperature in Celsius
            int humidity,          // Humidity percentage
            double windSpeed,      // Wind speed in m/s
            int cloudiness        // Cloudiness percentage
    ) {}

    /**
     * Cached weather entry.
     */
    private record CachedWeather(WeatherData data, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }

    /**
     * Calculate weather multiplier for the given location.
     * Returns 1.0 if API is disabled or on error.
     */
    public double calculateWeatherMultiplier(Location location) {
        if (!config.isUseWeatherApi() || !isApiKeyValid(config.getWeatherApiKey())) {
            log.debug("Weather API disabled or no API key, returning default multiplier 1.0");
            return 1.0;
        }

        try {
            WeatherData weather = getWeatherData(location);
            return calculateMultiplierFromWeather(weather);
        } catch (Exception e) {
            log.warn("Failed to get weather data, using default multiplier: {}", e.getMessage());
            return 1.0;
        }
    }

    /**
     * Get weather data for location (with caching).
     */
    public WeatherData getWeatherData(Location location) {
        String cacheKey = getCacheKey(location);

        // Check cache first
        CachedWeather cached = weatherCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached weather data for ({}, {})", location.getLat(), location.getLon());
            return cached.data();
        }

        // Fetch from API
        WeatherData weather = fetchWeatherFromApi(location);

        // Cache the result
        weatherCache.put(cacheKey, new CachedWeather(weather, System.currentTimeMillis()));

        return weather;
    }

    /**
     * Fetch weather data from OpenWeatherMap API.
     */
    @SuppressWarnings("unchecked")
    private WeatherData fetchWeatherFromApi(Location location) {
        String url = String.format(
                "%s?lat=%.6f&lon=%.6f&appid=%s&units=metric",
                config.getWeatherApiUrl(),
                location.getLat(),
                location.getLon(),
                config.getWeatherApiKey()
        );

        log.debug("Fetching weather from OpenWeatherMap for ({}, {})", location.getLat(), location.getLon());

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Invalid response from weather API: " + response.getStatusCode());
            }

            Map<String, Object> body = response.getBody();

            // Parse weather condition
            String condition = "Clear";
            String description = "clear sky";
            List<Map<String, Object>> weatherList = (List<Map<String, Object>>) body.get("weather");
            if (weatherList != null && !weatherList.isEmpty()) {
                Map<String, Object> weatherInfo = weatherList.get(0);
                condition = (String) weatherInfo.getOrDefault("main", "Clear");
                description = (String) weatherInfo.getOrDefault("description", "");
            }

            // Parse main data
            double temperature = 20.0;
            int humidity = 50;
            Map<String, Object> main = (Map<String, Object>) body.get("main");
            if (main != null) {
                temperature = toDouble(main.get("temp"), 20.0);
                humidity = toInt(main.get("humidity"), 50);
            }

            // Parse wind
            double windSpeed = 0.0;
            Map<String, Object> wind = (Map<String, Object>) body.get("wind");
            if (wind != null) {
                windSpeed = toDouble(wind.get("speed"), 0.0);
            }

            // Parse clouds
            int cloudiness = 0;
            Map<String, Object> clouds = (Map<String, Object>) body.get("clouds");
            if (clouds != null) {
                cloudiness = toInt(clouds.get("all"), 0);
            }

            log.info("Weather at ({:.4f}, {:.4f}): {} ({}), {:.1f}°C, humidity {}%, wind {:.1f} m/s",
                    location.getLat(), location.getLon(), condition, description,
                    temperature, humidity, windSpeed);

            return new WeatherData(condition, description, temperature, humidity, windSpeed, cloudiness);

        } catch (RestClientException e) {
            log.error("REST client error fetching weather: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch weather data: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate multiplier based on weather conditions.
     */
    private double calculateMultiplierFromWeather(WeatherData weather) {
        double multiplier = 1.0;
        String condition = weather.condition().toLowerCase();
        String description = weather.description().toLowerCase();

        // === PRECIPITATION CONDITIONS ===
        // Snow conditions (highest priority)
        if (condition.contains("snow") || description.contains("snow") ||
                description.contains("blizzard") || description.contains("sleet")) {
            multiplier = Math.max(multiplier, config.getSnowMultiplier());
            log.debug("Snow/sleet detected -> multiplier: {}", config.getSnowMultiplier());
        }
        // Heavy rain / thunderstorm
        else if (condition.contains("thunderstorm") ||
                description.contains("heavy rain") ||
                description.contains("extreme rain") ||
                description.contains("shower rain")) {
            multiplier = Math.max(multiplier, config.getHeavyRainMultiplier());
            log.debug("Heavy rain/storm detected -> multiplier: {}", config.getHeavyRainMultiplier());
        }
        // Light rain / drizzle
        else if (condition.contains("rain") || condition.contains("drizzle") ||
                description.contains("rain") || description.contains("drizzle")) {
            multiplier = Math.max(multiplier, config.getLightRainMultiplier());
            log.debug("Light rain/drizzle detected -> multiplier: {}", config.getLightRainMultiplier());
        }

        // === TEMPERATURE EXTREMES ===
        if (weather.temperature() > 35) {
            double heatMultiplier = config.getExtremeHeatMultiplier();
            multiplier = Math.max(multiplier, heatMultiplier);
            log.debug("Extreme heat ({:.1f}°C) -> multiplier: {}", weather.temperature(), heatMultiplier);
        } else if (weather.temperature() < 0) {
            double coldMultiplier = config.getExtremeColdMultiplier();
            multiplier = Math.max(multiplier, coldMultiplier);
            log.debug("Extreme cold ({:.1f}°C) -> multiplier: {}", weather.temperature(), coldMultiplier);
        }

        // === HIGH WINDS (optional additional factor) ===
        if (weather.windSpeed() > 15) { // > 15 m/s is strong wind
            // Add a small wind factor (could be configurable)
            multiplier *= 1.05;
            log.debug("Strong wind ({:.1f} m/s) -> additional 5%", weather.windSpeed());
        }

        log.debug("Final weather multiplier: {:.2f} (condition: {}, temp: {:.1f}°C)",
                multiplier, weather.condition(), weather.temperature());

        return multiplier;
    }

    /**
     * Generate cache key from location (rounded to 2 decimal places for locality).
     */
    private String getCacheKey(Location location) {
        // Round to ~1km precision for caching
        double lat = Math.round(location.getLat() * 100) / 100.0;
        double lon = Math.round(location.getLon() * 100) / 100.0;
        return lat + "," + lon;
    }

    /**
     * Check if API key is valid.
     */
    private boolean isApiKeyValid(String apiKey) {
        return apiKey != null && !apiKey.isBlank() && apiKey.length() > 10;
    }

    /**
     * Safe conversion to double.
     */
    private double toDouble(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Safe conversion to int.
     */
    private int toInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}