package com.driveme.backend.service;

import com.driveme.backend.common.Location;
import com.driveme.backend.common.Money;
import com.driveme.backend.common.RequestStatus;
import com.driveme.backend.dto.*;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.TripRequest;
import com.driveme.backend.entity.Vehicle;
import com.driveme.backend.helper.TripRequestMapper;
import com.driveme.backend.repository.PassengerRepository;
import com.driveme.backend.repository.TripRequestRepository;
import com.driveme.backend.repository.VehicleRepository;
import com.driveme.backend.service.pricing.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripRequestService {

    private final TripRequestRepository tripRequestRepository;
    private final PassengerRepository passengerRepository;
    private final VehicleRepository vehicleRepository;
    private final PricingService pricingService;

    private static final double MIN_DISTANCE_KM = 0.05;

    @Transactional
    public TripRequestDTO createTripRequest(UUID passengerId, CreateTripRequestRequest request) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));

        Vehicle vehicle = null;
        if (request.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + request.getVehicleId()));

            if (!vehicle.getPassenger().getId().equals(passengerId)) {
                throw new RuntimeException("Vehicle does not belong to the passenger");
            }
        }

        validateTripGeometry(request.getPickup(), request.getDestination());

        TripRequest tripRequest = TripRequestMapper.toEntity(request, passenger, vehicle);

        Location pickup = TripRequestMapper.toLocation(request.getPickup());
        Location destination = TripRequestMapper.toLocation(request.getDestination());

        PricingResult pricingResult = pricingService.calculatePrice(pickup, destination);

        tripRequest.setMinPrice(Money.ofTRY(pricingResult.getMinPrice()));
        tripRequest.setMaxPrice(Money.ofTRY(pricingResult.getMaxPrice()));

        // Set offer amount if provided
        if (request.getOfferAmount() != null) {
            tripRequest.setOfferAmount(Money.ofTRY(request.getOfferAmount()));
        }

        TripRequest savedRequest = tripRequestRepository.save(tripRequest);
        log.info("Trip request created with ID {} and price range {} - {}",
                savedRequest.getId(), pricingResult.getMinPrice(), pricingResult.getMaxPrice());
        return TripRequestMapper.toDTO(savedRequest);
    }

    @Transactional(readOnly = true)
    public TripRequestDTO getTripRequestById(UUID requestId) {
        TripRequest tripRequest = tripRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Trip request not found with id: " + requestId));
        return TripRequestMapper.toDTO(tripRequest);
    }

    @Transactional(readOnly = true)
    public List<TripRequestDTO> getTripRequestsByPassengerId(UUID passengerId) {
        return tripRequestRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId)
                .stream()
                .map(TripRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TripRequestDTO> getPendingTripRequests() {
        return tripRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING)
                .stream()
                .map(TripRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TripRequestDTO cancelTripRequest(UUID requestId, UUID passengerId) {
        TripRequest tripRequest = tripRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Trip request not found with id: " + requestId));

        if (!tripRequest.getPassenger().getId().equals(passengerId)) {
            throw new RuntimeException("You don't have permission to cancel this request");
        }

        tripRequest.cancel();
        TripRequest savedRequest = tripRequestRepository.save(tripRequest);
        return TripRequestMapper.toDTO(savedRequest);
    }

    private void validateTripGeometry(LocationDTO pickup, LocationDTO destination) {
        if (pickup == null || destination == null) {
            throw new IllegalArgumentException("Pickup and destination are required");
        }

        Location pickupLoc = TripRequestMapper.toLocation(pickup);
        Location destinationLoc = TripRequestMapper.toLocation(destination);
        double distanceKm = pickupLoc.distanceTo(destinationLoc);

        if (distanceKm < MIN_DISTANCE_KM) {
            throw new IllegalArgumentException("Pickup and destination cannot be the same location");
        }
    }
}