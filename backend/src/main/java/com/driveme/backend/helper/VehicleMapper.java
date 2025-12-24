package com.driveme.backend.helper;

import com.driveme.backend.dto.CreateVehicleRequest;
import com.driveme.backend.dto.VehicleDTO;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.Vehicle;

/**
 * Mapper for Vehicle entity and DTOs.
 */
public final class VehicleMapper {

    private VehicleMapper() {
        // Utility class
    }

    /**
     * Converts Vehicle entity to VehicleDTO.
     */
    public static VehicleDTO toDTO(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        return VehicleDTO.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .transmission(vehicle.getTransmission())
                .status(vehicle.getStatus())
                .passengerId(vehicle.getPassenger() != null ? vehicle.getPassenger().getId() : null)
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    /**
     * Creates a new Vehicle entity from CreateVehicleRequest.
     */
    public static Vehicle toEntity(CreateVehicleRequest request, Passenger passenger) {
        if (request == null) {
            return null;
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber(request.getPlateNumber());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setTransmission(request.getTransmission());
        vehicle.setPassenger(passenger);
        return vehicle;
    }
}

