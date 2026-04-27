package com.driveme.backend.repository;

import com.driveme.backend.common.TripStatus;
import com.driveme.backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Trip} — confirmed rides created at acceptance time.
 *
 * <p>We index by (driver_id, status) and (passenger_id, status) so the typical
 * "show me my active trip" / "my trip history" queries stay fast even with
 * large datasets.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    /** Lookup for the trip spawned from a given trip request (1-1 relation). */
    Optional<Trip> findByTripRequestId(UUID tripRequestId);

    /** All trips for a driver in newest-first order (history view). */
    List<Trip> findByDriverIdOrderByCreatedAtDesc(UUID driverId);

    /** All trips for a passenger in newest-first order (history view). */
    List<Trip> findByPassengerIdOrderByCreatedAtDesc(UUID passengerId);

    /** Currently active trips for a driver (e.g. ACCEPTED, DRIVER_ARRIVED, IN_PROGRESS). */
    List<Trip> findByDriverIdAndStatusIn(UUID driverId, List<TripStatus> statuses);

    /** Currently active trips for a passenger. */
    List<Trip> findByPassengerIdAndStatusIn(UUID passengerId, List<TripStatus> statuses);
}
