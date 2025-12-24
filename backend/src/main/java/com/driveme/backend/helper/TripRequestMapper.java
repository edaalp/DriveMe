package com.driveme.backend.helper;

import com.driveme.backend.common.Location;
import com.driveme.backend.common.Money;
import com.driveme.backend.dto.CreateTripRequestRequest;
import com.driveme.backend.dto.LocationDTO;
import com.driveme.backend.dto.TripRequestDTO;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.TripRequest;
import com.driveme.backend.entity.Vehicle;

/**
 * Mapper for TripRequest entity and DTOs.
 */
public final class TripRequestMapper {

    private TripRequestMapper() {
        // Utility class
    }

    /**
     * Converts TripRequest entity to TripRequestDTO.
     */
    public static TripRequestDTO toDTO(TripRequest tripRequest) {
        if (tripRequest == null) {
            return null;
        }

        return TripRequestDTO.builder()
                .id(tripRequest.getId())
                .requestedTime(tripRequest.getRequestedTime())
                .withPet(tripRequest.isWithPet())
                .status(tripRequest.getStatus())
                .minPriceAmount(tripRequest.getMinPrice() != null ? tripRequest.getMinPrice().getAmount() : null)
                .minPriceCurrency(tripRequest.getMinPrice() != null ? tripRequest.getMinPrice().getCurrency() : null)
                .maxPriceAmount(tripRequest.getMaxPrice() != null ? tripRequest.getMaxPrice().getAmount() : null)
                .maxPriceCurrency(tripRequest.getMaxPrice() != null ? tripRequest.getMaxPrice().getCurrency() : null)
                .pickup(toLocationDTO(tripRequest.getPickup()))
                .destination(toLocationDTO(tripRequest.getDestination()))
                .distanceKm(tripRequest.getDistanceKm())
                .passengerId(tripRequest.getPassenger() != null ? tripRequest.getPassenger().getId() : null)
                .vehicle(tripRequest.getVehicle() != null ? VehicleMapper.toDTO(tripRequest.getVehicle()) : null)
                .createdAt(tripRequest.getCreatedAt())
                .updatedAt(tripRequest.getUpdatedAt())
                .build();
    }

    /**
     * Creates a new TripRequest entity from CreateTripRequestRequest.
     */
    public static TripRequest toEntity(CreateTripRequestRequest request, Passenger passenger, Vehicle vehicle) {
        if (request == null) {
            return null;
        }

        TripRequest tripRequest = new TripRequest();
        tripRequest.setRequestedTime(request.getRequestedTime());
        tripRequest.setWithPet(request.isWithPet());
        tripRequest.setPickup(toLocation(request.getPickup()));
        tripRequest.setDestination(toLocation(request.getDestination()));
        tripRequest.setPassenger(passenger);
        tripRequest.setVehicle(vehicle);
        return tripRequest;
    }

    /**
     * Converts LocationDTO to Location embeddable.
     */
    public static Location toLocation(LocationDTO dto) {
        if (dto == null) {
            return null;
        }
        return new Location(dto.getLat(), dto.getLon(), dto.getAddressText());
    }

    /**
     * Converts Location embeddable to LocationDTO.
     */
    public static LocationDTO toLocationDTO(Location location) {
        if (location == null) {
            return null;
        }
        return LocationDTO.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .addressText(location.getAddressText())
                .build();
    }
}

