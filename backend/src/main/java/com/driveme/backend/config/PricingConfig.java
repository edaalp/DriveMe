package com.driveme.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Pricing configuration with Ankara Taxi Official Tariff (March 2026).
 */
@ConfigurationProperties(prefix = "pricing")
@Component
@Data
public class PricingConfig {

    // ========== ANKARA TAXI OFFICIAL TARIFF (MARCH 2026) ==========
    // Source: Ankara Municipality - effective 1 March 2026
    
    /** Taksiye Bindi-İndi ücreti (minimum fare) */
    private double taxiMinFare = 200.0;              // TRY
    
    /** Açılış Ücreti (opening/flag-down fee) */
    private double taxiOpeningFee = 65.0;            // TRY
    
    /** Kilometre Ücreti (per km rate) */
    private double taxiPerKmRate = 40.0;             // TRY/km
    
    /** Saat ücreti (hourly waiting rate) */
    private double taxiHourlyRate = 420.0;           // TRY/hour
    
    /** Birim Zaman Ücreti (per minute rate) */
    private double taxiPerMinuteRate = 7.0;          // TRY/min
    
    /** Birim Mesafe Ücreti (per 100m rate, for precision) */
    private double taxiPer100mRate = 4.0;            // TRY/100m

    // ========== DRIVER COMPENSATION ==========
    /** 
     * Return trip compensation factor.
     * 1.0 = full return trip, 0.7 = 70% of forward trip cost
     * Accounts for driver needing to return or find another ride
     */
    private double driverReturnFactor = 0.7;

    // ========== SERVICE FEE ==========
    /** Platform service fee percentage */
    private double servicePercentage = 0.15;         // 15%

    // ========== PRICE RANGE BAND ==========
    private double minPricePercentage = 0.9;
    private double maxPricePercentage = 1.1;
    private String currency = "TRY";

    // ========== TIME MULTIPLIERS ==========
    // Rush Hours - Morning
    private int rushHourStartMorning = 7;
    private int rushHourEndMorning = 10;
    // Rush Hours - Evening
    private int rushHourStartEvening = 17;
    private int rushHourEndEvening = 20;
    private double rushHourMultiplier = 1.25;
    
    // Night Time
    private int nightTimeStart = 22;
    private int nightTimeEnd = 6;
    private double nightTimeMultiplier = 1.15;
    
    // Weekend
    private double weekendMultiplier = 1.1;

    // ========== DEMAND/SURGE MULTIPLIERS ==========
    private double maxSurgeMultiplier = 3.0;
    private double minSurgeMultiplier = 1.0;
    /** For every X% demand over supply, add surge increment */
    private double surgeStepThreshold = 0.2;         // 20%
    private double surgeStepIncrement = 0.25;        // +0.25 per step

    // ========== WEATHER MULTIPLIERS ==========
    private double lightRainMultiplier = 1.1;
    private double heavyRainMultiplier = 1.25;
    private double snowMultiplier = 1.35;
    private double extremeHeatMultiplier = 1.1;      // > 35°C
    private double extremeColdMultiplier = 1.15;     // < 0°C

    // ========== TRAFFIC MULTIPLIERS ==========
    private double moderateTrafficMultiplier = 1.1;
    private double heavyTrafficMultiplier = 1.25;
    private double severeTrafficMultiplier = 1.4;

    // ========== SPECIAL LOCATION FACTORS ==========
    /** Return factor for popular destinations (airport, malls, etc.) */
    private double popularDestinationReturnFactor = 0.3;

    // ========== API CONFIGURATION ==========
    private boolean useGoogleMaps = false;
    private String googleMapsApiKey = "";
    private boolean useTrafficData = false;
    
    private boolean useWeatherApi = false;
    private String weatherApiKey = "";
    private String weatherApiUrl = "https://api.openweathermap.org/data/2.5/weather";
}