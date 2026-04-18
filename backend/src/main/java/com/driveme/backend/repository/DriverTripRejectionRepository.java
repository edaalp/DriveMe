package com.driveme.backend.repository;

import com.driveme.backend.entity.DriverTripRejection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DriverTripRejection entity operations.
 */
@Repository
public interface DriverTripRejectionRepository extends JpaRepository<DriverTripRejection, UUID> {

    /**
     * Find if a driver has rejected a specific trip.
     */
    Optional<DriverTripRejection> findByTripRequestIdAndDriverId(UUID tripRequestId, UUID driverId);

    /**
     * Check if a driver has rejected a specific trip.
     */
    boolean existsByTripRequestIdAndDriverId(UUID tripRequestId, UUID driverId);

    /**
     * Find all rejections for a specific trip.
     */
    List<DriverTripRejection> findByTripRequestId(UUID tripRequestId);

    /**
     * Find all rejections by a specific driver.
     */
    List<DriverTripRejection> findByDriverId(UUID driverId);
}

