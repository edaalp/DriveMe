package com.driveme.backend.service;

import com.driveme.backend.common.Location;
import com.driveme.backend.common.Money;
import com.driveme.backend.common.RequestStatus;
import com.driveme.backend.dto.CreateTripRequestRequest;
import com.driveme.backend.dto.LocationDTO;
import com.driveme.backend.dto.PriceRangeDTO;
import com.driveme.backend.dto.TripRequestDTO;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.TripRequest;
import com.driveme.backend.entity.Vehicle;
import com.driveme.backend.helper.TripRequestMapper;
import com.driveme.backend.repository.PassengerRepository;
import com.driveme.backend.repository.TripRequestRepository;
import com.driveme.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for TripRequest operations.
 */
@Service
@RequiredArgsConstructor
public class TripRequestService {

    private final TripRequestRepository tripRequestRepository;
    private final PassengerRepository passengerRepository;
    private final VehicleRepository vehicleRepository;

    // Price calculation constants (TL per km)
    private static final BigDecimal MIN_PRICE_PER_KM = new BigDecimal("12.00");
    private static final BigDecimal MAX_PRICE_PER_KM = new BigDecimal("18.00");
    private static final BigDecimal MIN_BASE_PRICE = new BigDecimal("50.00");

    /**
     * Create a new trip request.
     */
    @Transactional
    public TripRequestDTO createTripRequest(UUID passengerId, CreateTripRequestRequest request) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));

        // Get vehicle if provided
        Vehicle vehicle = null;
        if (request.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + request.getVehicleId()));

            // Verify ownership
            if (!vehicle.getPassenger().getId().equals(passengerId)) {
                throw new RuntimeException("Vehicle does not belong to the passenger");
            }
        }

        // Create trip request
        TripRequest tripRequest = TripRequestMapper.toEntity(request, passenger, vehicle);

        // Calculate price range based on distance
        PriceRangeDTO priceRange = calculatePriceRange(request.getPickup(), request.getDestination());
        tripRequest.setMinPrice(Money.ofTRY(priceRange.getMinPrice()));
        tripRequest.setMaxPrice(Money.ofTRY(priceRange.getMaxPrice()));

        TripRequest savedRequest = tripRequestRepository.save(tripRequest);
        return TripRequestMapper.toDTO(savedRequest);
    }

    /**
     * Get a trip request by ID.
     */
    @Transactional(readOnly = true)
    public TripRequestDTO getTripRequestById(UUID requestId) {
        TripRequest tripRequest = tripRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Trip request not found with id: " + requestId));
        return TripRequestMapper.toDTO(tripRequest);
    }

    /**
     * Get all trip requests for a passenger.
     */
    @Transactional(readOnly = true)
    public List<TripRequestDTO> getTripRequestsByPassengerId(UUID passengerId) {
        return tripRequestRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId)
                .stream()
                .map(TripRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all pending trip requests (for drivers to see).
     */
    @Transactional(readOnly = true)
    public List<TripRequestDTO> getPendingTripRequests() {
        return tripRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING)
                .stream()
                .map(TripRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Cancel a trip request.
     */
    @Transactional
    public TripRequestDTO cancelTripRequest(UUID requestId, UUID passengerId) {
        TripRequest tripRequest = tripRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Trip request not found with id: " + requestId));

        // Verify ownership
        if (!tripRequest.getPassenger().getId().equals(passengerId)) {
            throw new RuntimeException("You don't have permission to cancel this request");
        }

        tripRequest.cancel();
        TripRequest savedRequest = tripRequestRepository.save(tripRequest);
        return TripRequestMapper.toDTO(savedRequest);
    }

    /**
     * Calculate price range based on pickup and destination locations.
     */
    public PriceRangeDTO calculatePriceRange(LocationDTO pickup, LocationDTO destination) {
        Location pickupLoc = TripRequestMapper.toLocation(pickup);
        Location destLoc = TripRequestMapper.toLocation(destination);

        double distanceKm = pickupLoc.distanceTo(destLoc);

        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        BigDecimal minPrice = MIN_PRICE_PER_KM.multiply(distance).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxPrice = MAX_PRICE_PER_KM.multiply(distance).setScale(2, RoundingMode.HALF_UP);

        // Ensure minimum base price
        if (minPrice.compareTo(MIN_BASE_PRICE) < 0) {
            minPrice = MIN_BASE_PRICE;
        }
        if (maxPrice.compareTo(MIN_BASE_PRICE) < 0) {
            maxPrice = MIN_BASE_PRICE.add(new BigDecimal("20.00"));
        }

        return PriceRangeDTO.ofTRY(minPrice, maxPrice, distanceKm);
    }

    /**
     * Calculate price range based on distance in km.
     */
    public PriceRangeDTO calculatePriceRangeByDistance(double distanceKm) {
        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        BigDecimal minPrice = MIN_PRICE_PER_KM.multiply(distance).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxPrice = MAX_PRICE_PER_KM.multiply(distance).setScale(2, RoundingMode.HALF_UP);

        // Ensure minimum base price
        if (minPrice.compareTo(MIN_BASE_PRICE) < 0) {
            minPrice = MIN_BASE_PRICE;
        }
        if (maxPrice.compareTo(MIN_BASE_PRICE) < 0) {
            maxPrice = MIN_BASE_PRICE.add(new BigDecimal("20.00"));
        }

        return PriceRangeDTO.ofTRY(minPrice, maxPrice, distanceKm);
    }
}

