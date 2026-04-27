package com.driveme.backend.helper;

import com.driveme.backend.dto.TripDTO;
import com.driveme.backend.entity.BaseUser;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.Trip;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Entity ↔ DTO mapper for {@link Trip}.
 *
 * <p>Kept as a Spring component (not a static utility) because it needs to
 * inject {@code app.public-base-url} to build absolute URLs for party
 * avatars — same pattern used by {@code DriverMapper}.
 */
@Component
public class TripMapper {

    @Value("${app.public-base-url:http://10.0.2.2:8080}")
    private String publicBaseUrl;

    /**
     * Converts a {@link Trip} entity to its wire DTO. Returns {@code null} if
     * the entity itself is null so callers don't have to guard.
     */
    public TripDTO toDTO(Trip trip) {
        if (trip == null) {
            return null;
        }

        return TripDTO.builder()
                // Identity
                .id(trip.getId())
                .tripRequestId(trip.getTripRequestId())
                .status(trip.getStatus())

                // Parties
                .driver(toParty(trip.getDriver()))
                .passenger(toParty(trip.getPassenger()))
                .vehicle(trip.getVehicle() != null ? VehicleMapper.toDTO(trip.getVehicle()) : null)

                // Route
                .pickup(TripRequestMapper.toLocationDTO(trip.getPickup()))
                .destination(TripRequestMapper.toLocationDTO(trip.getDestination()))
                .plannedDistanceKm(trip.getPlannedDistanceKm())
                .plannedDurationMinutes(trip.getPlannedDurationMinutes())
                .actualDistanceKm(trip.getActualDistanceKm())
                .actualDurationMinutes(trip.getActualDurationMinutes())
                .routePolyline(trip.getRoutePolyline())

                // Money
                .agreedAmount(trip.getAgreedAmount() != null ? trip.getAgreedAmount().getAmount() : null)
                .agreedCurrency(trip.getAgreedAmount() != null ? trip.getAgreedAmount().getCurrency() : null)
                .finalAmount(trip.getFinalAmount() != null ? trip.getFinalAmount().getAmount() : null)
                .finalCurrency(trip.getFinalAmount() != null ? trip.getFinalAmount().getCurrency() : null)
                .penaltyAmount(trip.getPenaltyAmount() != null ? trip.getPenaltyAmount().getAmount() : null)
                .penaltyCurrency(trip.getPenaltyAmount() != null ? trip.getPenaltyAmount().getCurrency() : null)
                .paymentStatus(trip.getPaymentStatus())
                .paymentMethod(trip.getPaymentMethod())

                // Preferences
                .withPet(trip.isWithPet())

                // Lifecycle
                .acceptedAt(trip.getAcceptedAt())
                .driverArrivedAt(trip.getDriverArrivedAt())
                .startedAt(trip.getStartedAt())
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .cancellationReason(trip.getCancellationReason())

                // Live tracking
                .driverLastLat(trip.getDriverLastLat())
                .driverLastLon(trip.getDriverLastLon())
                .etaToPickupMinutes(trip.getEtaToPickupMinutes())

                // Boarding verification
                .verificationCode(trip.getVerificationCode())

                // Ratings
                .ratingByPassenger(trip.getRatingByPassenger())
                .ratingByDriver(trip.getRatingByDriver())
                .commentByPassenger(trip.getCommentByPassenger())
                .commentByDriver(trip.getCommentByDriver())

                // Audit
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())

                .build();
    }

    /**
     * Builds a small {@link TripDTO.PartyDTO} projection for a driver or passenger.
     * Keeps avatar URLs absolute so the mobile app can render them directly
     * without knowing the backend base host.
     */
    private TripDTO.PartyDTO toParty(BaseUser user) {
        if (user == null) {
            return null;
        }

        String photoUrl = switch (user) {
            case Driver d -> d.getProfilePictureUrl();
            case Passenger p -> p.getProfilePictureUrl();
            default -> null;
        };

        return TripDTO.PartyDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(toAbsoluteUrl(photoUrl))
                .avgRating(user.getAvgRating())
                .ratingCount(user.getRatingCount())
                .build();
    }

    /**
     * Turns a stored relative path (e.g. {@code /uploads/foo.jpg}) into an
     * absolute URL the mobile client can hit directly. Pass-through for
     * already-absolute URLs.
     */
    private String toAbsoluteUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return null;
        }
        String trimmed = pathOrUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        String path = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return base + path;
    }
}
