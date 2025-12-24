package com.driveme.backend.repository;

import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Vehicle entity operations.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    /**
     * Find all vehicles owned by a passenger.
     */
    List<Vehicle> findByPassengerId(UUID passengerId);

    /**
     * Find a vehicle by its plate number.
     */
    Optional<Vehicle> findByPlateNumber(String plateNumber);

    /**
     * Find vehicles by verification status.
     */
    List<Vehicle> findByStatus(VerificationStatus status);

    /**
     * Check if a passenger already has a vehicle with the given plate number.
     */
    boolean existsByPassengerIdAndPlateNumber(UUID passengerId, String plateNumber);

    /**
     * Check if a plate number is already registered.
     */
    boolean existsByPlateNumber(String plateNumber);
}

