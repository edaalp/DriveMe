package com.driveme.backend.config;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * Configuration properties for pricing calculation.
 * Can be loaded from application.yaml or environment variables.
 */
@Component
@ConfigurationProperties(prefix = "pricing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingConfig {
    @Builder.Default
    private double baseFare = 20.0;
    @Builder.Default
    private double perKmRate = 8.0;
    @Builder.Default
    private double perMinuteRate = 1.0;
    @Builder.Default
    private double maxSurgeMultiplier = 3.0;
    @Builder.Default
    private String currency = "TRY";
    @Builder.Default
    private double minPricePercentage = 0.9;
    @Builder.Default
    private double maxPricePercentage = 1.1;
    private String googleMapsApiKey;
    @Builder.Default
    private boolean useGoogleMaps = false;
    @Builder.Default
    private int rushHourStartMorning = 7;
    @Builder.Default
    private int rushHourEndMorning = 10;
    @Builder.Default
    private int rushHourStartEvening = 17;
    @Builder.Default
    private int rushHourEndEvening = 20;
    @Builder.Default
    private double rushHourMultiplier = 1.2;
    @Builder.Default
    private double nightTimeMultiplier = 1.1;
    @Builder.Default
    private int nightTimeStart = 22;
    @Builder.Default
    private int nightTimeEnd = 6;
}