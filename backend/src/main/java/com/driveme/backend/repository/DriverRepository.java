package com.driveme.backend.repository;

import com.driveme.backend.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Driver entity.
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    /**
     * Find driver by email.
     */
    Optional<Driver> findByEmail(String email);

    /**
     * Find driver by username.
     */
    Optional<Driver> findByUserName(String userName);

    /**
     * Find driver by license number.
     */
    Optional<Driver> findByLicenseNumber(String licenseNumber);

    /**
     * Find available drivers.
     */
    java.util.List<Driver> findByIsAvailableTrue();

    /**
     * Find active drivers.
     */
    List<Driver> findByActiveTrue();

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Check if username exists.
     */
    boolean existsByUserName(String userName);
}
