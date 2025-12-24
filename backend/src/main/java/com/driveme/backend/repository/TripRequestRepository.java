package com.driveme.backend.repository;

import com.driveme.backend.common.RequestStatus;
import com.driveme.backend.entity.TripRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for TripRequest entity operations.
 */
@Repository
public interface TripRequestRepository extends JpaRepository<TripRequest, UUID> {

    /**
     * Find all trip requests by passenger ID.
     */
    List<TripRequest> findByPassengerId(UUID passengerId);

    /**
     * Find all trip requests by passenger ID ordered by creation date descending.
     */
    List<TripRequest> findByPassengerIdOrderByCreatedAtDesc(UUID passengerId);

    /**
     * Find all trip requests with a specific status.
     */
    List<TripRequest> findByStatus(RequestStatus status);

    /**
     * Find pending trip requests (for drivers to see available requests).
     */
    List<TripRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    /**
     * Find active trip requests for a passenger (pending or matched).
     */
    List<TripRequest> findByPassengerIdAndStatusIn(UUID passengerId, List<RequestStatus> statuses);
}

