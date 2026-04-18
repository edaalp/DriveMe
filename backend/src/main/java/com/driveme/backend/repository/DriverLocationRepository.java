package com.driveme.backend.repository;

import com.driveme.backend.entity.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DriverLocation entity operations.
 */
@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, UUID> {

    /**
     * Find driver location by driver ID.
     */
    Optional<DriverLocation> findByDriverId(UUID driverId);
}

