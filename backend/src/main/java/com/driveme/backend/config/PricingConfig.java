package com.driveme.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "pricing")
@Component
@Data
public class PricingConfig {

    // Ankara Taxi Official Tariff (2026)
    private double taxiOpeningFee = 55.0;        // TRY midpoint of 45–65 range
    private double taxiPerKmRate = 36.0;          // TRY/km midpoint of 32–40 range
    private double taxiPerMinuteRate = 7.0;       // TRY/min waiting rate
    private double taxiMinFare = 175.0;           // TRY midpoint of indi-bindi 150–200 range

    // Service fee: percentage of total taxi cost (both legs)
    private double servicePercentage = 0.15;      // 15% — override as needed

    // Price range band
    private double minPricePercentage = 0.9;
    private double maxPricePercentage = 1.1;

    private String currency = "TRY";
    private double maxSurgeMultiplier = 3.0;

    // Time multipliers
    private int rushHourStartMorning = 7;
    private int rushHourEndMorning = 10;
    private int rushHourStartEvening = 17;
    private int rushHourEndEvening = 20;
    private double rushHourMultiplier = 1.2;
    private int nightTimeStart = 22;
    private int nightTimeEnd = 6;
    private double nightTimeMultiplier = 1.1;

    // Google Maps
    private boolean useGoogleMaps = false;
    private String googleMapsApiKey = "";
}