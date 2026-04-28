package com.driveme.backend.service.pricing;

import com.driveme.backend.config.PricingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Service for calculating time-based pricing multipliers.
 * Considers: rush hour, night time, weekends.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimePricingService {

    private final PricingConfig config;

    /**
     * Result record containing multiplier and reason.
     */
    public record TimeMultiplierResult(
        double multiplier,
        String reason
    ) {}

    /**
     * Calculate combined time-based multiplier for the given datetime.
     * 
     * @param dateTime the date/time to calculate multiplier for (null = now)
     * @return TimeMultiplierResult with multiplier and reason
     */
    public TimeMultiplierResult calculateTimeMultiplier(LocalDateTime dateTime) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        }

        int hour = dateTime.getHour();
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();

        double multiplier = 1.0;
        StringBuilder reason = new StringBuilder();

        // Rush hour check (morning: 07:00 - 10:00)
        if (hour >= config.getRushHourStartMorning() && hour < config.getRushHourEndMorning()) {
            multiplier = Math.max(multiplier, config.getRushHourMultiplier());
            reason.append("Sabah trafiği (07-10); ");
        }
        // Rush hour check (evening: 17:00 - 20:00)
        else if (hour >= config.getRushHourStartEvening() && hour < config.getRushHourEndEvening()) {
            multiplier = Math.max(multiplier, config.getRushHourMultiplier());
            reason.append("Akşam trafiği (17-20); ");
        }
        // Night time check (22:00 - 06:00)
        else if (hour >= config.getNightTimeStart() || hour < config.getNightTimeEnd()) {
            multiplier = Math.max(multiplier, config.getNightTimeMultiplier());
            reason.append("Gece tarifesi (22-06); ");
        }

        // Weekend check (Saturday or Sunday)
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            // Weekend multiplier stacks with time multiplier
            multiplier *= config.getWeekendMultiplier();
            reason.append("Hafta sonu; ");
        }

        String reasonStr = reason.length() > 0 
            ? reason.toString().trim().replaceAll(";\\s*$", "") 
            : "Normal saat";
        
        log.debug("Time multiplier: {} ({})", multiplier, reasonStr);

        return new TimeMultiplierResult(multiplier, reasonStr);
    }
}
