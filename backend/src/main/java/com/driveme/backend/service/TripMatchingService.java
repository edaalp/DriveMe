package com.driveme.backend.service;

import com.driveme.backend.common.Location;
import com.driveme.backend.common.RequestStatus;
import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.dto.AvailableTripRequestDTO;
import com.driveme.backend.dto.MatchingStatusDTO;
import com.driveme.backend.dto.TripAcceptanceResponseDTO;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.entity.DriverTripRejection;
import com.driveme.backend.entity.Trip;
import com.driveme.backend.entity.TripRequest;
import com.driveme.backend.helper.TripRequestMapper;
import com.driveme.backend.helper.VehicleMapper;
import com.driveme.backend.repository.DriverRepository;
import com.driveme.backend.repository.DriverTripRejectionRepository;
import com.driveme.backend.repository.TripRepository;
import com.driveme.backend.repository.TripRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for trip request matching operations.
 */
@Service
@RequiredArgsConstructor
public class TripMatchingService {

    private final TripRequestRepository tripRequestRepository;
    private final TripRepository tripRepository;
    private final DriverRepository driverRepository;
    private final DriverTripRejectionRepository driverTripRejectionRepository;
    private final TripService tripService;

    /**
     * Get available trip requests for a driver based on location and radii.
     * Filters out already rejected trips and matched trips.
     */
    @Transactional(readOnly = true)
    public List<AvailableTripRequestDTO> getAvailableTripsForDriver(
            UUID driverId,
            double driverLat,
            double driverLon,
            double pickupRadiusKm,
            double tripRadiusKm) {

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        // Driver must be available
        if (!driver.isAvailable()) {
            return new ArrayList<>();
        }

        // Get all pending trips
        List<TripRequest> pendingTrips = tripRequestRepository.findByStatus(RequestStatus.PENDING);

        Location driverLocation = new Location(driverLat, driverLon, null);

        return pendingTrips.stream()
                // Filter by pickup radius
                .filter(trip -> {
                    if (trip.getPickup() == null) return false;
                    double distanceToPickup = driverLocation.distanceTo(trip.getPickup());
                    return distanceToPickup <= pickupRadiusKm;
                })
                // Filter by trip radius (pickup to destination)
                .filter(trip -> {
                    double tripDistance = trip.getDistanceKm();
                    return tripDistance <= tripRadiusKm;
                })
                // Filter out rejected trips
                .filter(trip -> !driverTripRejectionRepository.existsByTripRequestIdAndDriverId(trip.getId(), driverId))
                // Filter by pet acceptance
                .filter(trip -> !trip.isWithPet() || driver.isAcceptsPets())
                // Map to DTO
                .map(this::mapToAvailableTripDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get matching status for a trip request (passenger view).
     */
    @Transactional(readOnly = true)
    public MatchingStatusDTO getMatchingStatus(UUID tripId) {
        TripRequest tripRequest = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip request not found with id: " + tripId));

        MatchingStatusDTO.MatchingStatusDTOBuilder builder = MatchingStatusDTO.builder()
                .tripId(tripId)
                .status(tripRequest.getStatus());

        // If matched, include driver info and the Trip entity id.
        if (tripRequest.getStatus() == RequestStatus.MATCHED && tripRequest.getMatchedDriver() != null) {
            Driver driver = tripRequest.getMatchedDriver();
            int etaMinutes = Math.max(1, (int) Math.round(tripRequest.getDistanceKm()));
            MatchingStatusDTO.DriverInfoDTO driverInfo = MatchingStatusDTO.DriverInfoDTO.builder()
                    .id(driver.getId())
                    .fullName(driver.getFullName())
                    .avgRating(driver.getAvgRating() != null ? driver.getAvgRating() : 0.0)
                    .etaMinutes(etaMinutes)
                    .vehicleDescription(driver.getVehicleDescription())
                    .acceptsPets(driver.isAcceptsPets())
                    .build();
            builder.matchedDriver(driverInfo);

            // Include the Trip entity id so the passenger client can poll trip status directly.
            tripRepository.findByTripRequestId(tripRequest.getId())
                    .ifPresent(trip -> builder.tripEntityId(trip.getId()));
        }

        // For now, set counters to sensible defaults
        // In a production system, these would be tracked separately
        builder.notifiedDriversCount(0)
               .viewingDriversCount(0)
               .respondedDriversCount(0);

        return builder.build();
    }

    /**
     * Accept a trip request as a driver.
     */
    @Transactional
    public TripAcceptanceResponseDTO acceptTrip(UUID tripId, UUID driverId) {
        TripRequest tripRequest = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip request not found with id: " + tripId));

        // Verify trip is still pending
        if (tripRequest.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Trip request is not available for acceptance. Status: " + tripRequest.getStatus());
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        // Verify driver is available
        if (!driver.isAvailable()) {
            throw new RuntimeException("Driver is not available to accept trips");
        }

        // Verify driver is verified
        if (driver.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new RuntimeException("Driver must be verified to accept trips");
        }

        // Mark trip as matched
        tripRequest.markMatched(driver);
        TripRequest savedTrip = tripRequestRepository.save(tripRequest);

        // Create the confirmed Trip record within the same transaction so both
        // writes are atomic. The Trip carries the binding ride contract from
        // here on — the TripRequest becomes an immutable historical artefact.
        Trip trip = tripService.createFromAcceptedRequest(savedTrip, driver);

        // Build response
        return TripAcceptanceResponseDTO.builder()
                .tripId(tripId)
                .tripEntityId(trip.getId())
                .message("Trip accepted successfully")
                .success(true)
                .tripDetails(TripRequestMapper.toDTO(savedTrip))
                .driverInfo(TripAcceptanceResponseDTO.DriverInfoDTO.builder()
                        .id(driver.getId())
                        .fullName(driver.getFullName())
                        .phoneNumber(driver.getPhoneNumber())
                        .avgRating(driver.getAvgRating() != null ? driver.getAvgRating() : 0.0)
                        .build())
                .build();
    }

    /**
     * Reject a trip request as a driver.
     * Records the rejection so the trip is not shown to this driver again.
     */
    @Transactional
    public void rejectTrip(UUID tripId, UUID driverId) {
        TripRequest tripRequest = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip request not found with id: " + tripId));

        // Verify trip is still pending
        if (tripRequest.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Cannot reject a trip that is not pending");
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        // Record the rejection if not already rejected
        if (!driverTripRejectionRepository.existsByTripRequestIdAndDriverId(tripId, driverId)) {
            DriverTripRejection rejection = new DriverTripRejection();
            rejection.setTripRequest(tripRequest);
            rejection.setDriver(driver);
            rejection.setRejectedAt(Instant.now());
            driverTripRejectionRepository.save(rejection);
        }
    }

    /**
     * Cancel a trip request (passenger initiates cancellation).
     * Used as alternative to DELETE endpoint if needed.
     */
    @Transactional
    public void cancelTrip(UUID tripId, UUID passengerId) {
        TripRequest tripRequest = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip request not found with id: " + tripId));

        // Verify ownership
        if (!tripRequest.getPassenger().getId().equals(passengerId)) {
            throw new RuntimeException("You don't have permission to cancel this request");
        }

        tripRequest.cancel();
        tripRequestRepository.save(tripRequest);
    }

    /**
     * Map TripRequest entity to AvailableTripRequestDTO for driver view.
     */
    private AvailableTripRequestDTO mapToAvailableTripDTO(TripRequest trip) {
        // Estimate time: assume 60 km/h average speed
        long estimatedMinutes = Math.round((trip.getDistanceKm() / 60.0) * 60);

        double passengerRating = 0.0;
        if (trip.getPassenger() != null && trip.getPassenger().getAvgRating() != null) {
            passengerRating = trip.getPassenger().getAvgRating();
        }

        return AvailableTripRequestDTO.builder()
                .tripId(trip.getId())
                .requestedTime(trip.getRequestedTime() != null
                        ? trip.getRequestedTime()
                        : trip.getCreatedAt())
                .passengerName(trip.getPassenger().getFullName())
                .passengerRating(passengerRating)
                .passengerTripCount(0) // TODO: Count passenger's completed trips
                .withPet(trip.isWithPet())
                .pickup(TripRequestMapper.toLocationDTO(trip.getPickup()))
                .destination(TripRequestMapper.toLocationDTO(trip.getDestination()))
                .distanceKm(trip.getDistanceKm())
                .estimatedMinutes(estimatedMinutes)
                .offerAmount(trip.getOfferAmount() != null ? trip.getOfferAmount().getAmount() : null)
                .offerCurrency(trip.getOfferAmount() != null ? trip.getOfferAmount().getCurrency() : null)
                .vehicle(trip.getVehicle() != null ? VehicleMapper.toDTO(trip.getVehicle()) : null)
                .build();
    }
}








