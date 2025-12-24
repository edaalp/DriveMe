package com.driveme.backend.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable location value object with coordinates and address.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lon;

    @Column(length = 500)
    private String addressText;

    /**
     * Calculates the distance to another location using Haversine formula.
     * @param other the other location
     * @return distance in kilometers
     */
    public double distanceTo(Location other) {
        if (other == null) {
            return 0;
        }

        final int EARTH_RADIUS_KM = 6371;

        double latDistance = Math.toRadians(other.lat - this.lat);
        double lonDistance = Math.toRadians(other.lon - this.lon);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}

