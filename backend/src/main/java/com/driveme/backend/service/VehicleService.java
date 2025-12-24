package com.driveme.backend.service;

import com.driveme.backend.dto.CreateVehicleRequest;
import com.driveme.backend.dto.VehicleDTO;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.Vehicle;
import com.driveme.backend.helper.VehicleMapper;
import com.driveme.backend.repository.PassengerRepository;
import com.driveme.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Vehicle operations.
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final PassengerRepository passengerRepository;

    /**
     * Get all vehicles for a passenger.
     */
    @Transactional(readOnly = true)
    public List<VehicleDTO> getVehiclesByPassengerId(UUID passengerId) {
        return vehicleRepository.findByPassengerId(passengerId)
                .stream()
                .map(VehicleMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a vehicle by ID.
     */
    @Transactional(readOnly = true)
    public VehicleDTO getVehicleById(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));
        return VehicleMapper.toDTO(vehicle);
    }

    /**
     * Create a new vehicle for a passenger.
     */
    @Transactional
    public VehicleDTO createVehicle(UUID passengerId, CreateVehicleRequest request) {
        // Check if plate number already exists
        if (vehicleRepository.existsByPlateNumber(request.getPlateNumber())) {
            throw new RuntimeException("A vehicle with this plate number already exists");
        }

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));

        Vehicle vehicle = VehicleMapper.toEntity(request, passenger);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return VehicleMapper.toDTO(savedVehicle);
    }

    /**
     * Delete a vehicle.
     */
    @Transactional
    public void deleteVehicle(UUID vehicleId, UUID passengerId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        // Check ownership
        if (!vehicle.getPassenger().getId().equals(passengerId)) {
            throw new RuntimeException("You don't have permission to delete this vehicle");
        }

        vehicleRepository.delete(vehicle);
    }

    /**
     * Check if a vehicle belongs to a passenger.
     */
    @Transactional(readOnly = true)
    public boolean isVehicleOwnedByPassenger(UUID vehicleId, UUID passengerId) {
        return vehicleRepository.findById(vehicleId)
                .map(v -> v.getPassenger().getId().equals(passengerId))
                .orElse(false);
    }
}

