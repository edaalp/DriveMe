package com.driveme.backend.service.pricing;

import com.driveme.backend.config.PricingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Service for calculating time-based pricing multipliers.
 * Applies multipliers for rush hours and night time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimePricingService {

    private final PricingConfig pricingConfig;

    /**
     * Calculate time-based multiplier based on the requested time.
     *
     * @param requestedTime the time the trip is requested for
     * @return time multiplier (1.0 = normal time, > 1.0 = surge time)
     */
    public double calculateTimeMultiplier(Instant requestedTime) {
        if (requestedTime == null) {
            requestedTime = Instant.now();
        }

        try {
            // Convert to local time (Turkish timezone or UTC, depending on your needs)
            ZonedDateTime zonedTime = requestedTime.atZone(ZoneId.of("Europe/Istanbul"));
            int hour = zonedTime.getHour();

            // Check for night time
            if (isNightTime(hour)) {
                log.debug("Night time detected for hour {}, applying multiplier {}", hour, pricingConfig.getNightTimeMultiplier());
                return pricingConfig.getNightTimeMultiplier();
            }

            // Check for rush hour
            if (isRushHour(hour)) {
                log.debug("Rush hour detected for hour {}, applying multiplier {}", hour, pricingConfig.getRushHourMultiplier());
                return pricingConfig.getRushHourMultiplier();
            }

            // Normal time
            log.debug("Normal time for hour {}", hour);
            return 1.0;
        } catch (Exception e) {
            log.warn("Error calculating time multiplier, returning 1.0", e);
            return 1.0;
        }
    }

    /**
     * Check if the given hour falls within rush hour times.
     * Rush hours: 07:00-10:00 and 17:00-20:00
     *
     * @param hour the hour (0-23)
     * @return true if it's rush hour
     */
    private boolean isRushHour(int hour) {
        return (hour >= pricingConfig.getRushHourStartMorning() && hour < pricingConfig.getRushHourEndMorning()) ||
               (hour >= pricingConfig.getRushHourStartEvening() && hour < pricingConfig.getRushHourEndEvening());
    }

    /**
     * Check if the given hour falls within night time.
     * Night time: 22:00-06:00
     *
     * @param hour the hour (0-23)
     * @return true if it's night time
     */
    private boolean isNightTime(int hour) {
        int nightStart = pricingConfig.getNightTimeStart();
        int nightEnd = pricingConfig.getNightTimeEnd();

        if (nightStart < nightEnd) {
            // e.g., 14:00-18:00
            return hour >= nightStart && hour < nightEnd;
        } else {
            // e.g., 22:00-06:00 (spans midnight)
            return hour >= nightStart || hour < nightEnd;
        }
    }
}

