package com.driveme.backend.repository;

import com.driveme.backend.dto.TripDTO;
import com.driveme.backend.entity.Driver;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.TripEntity;
import com.driveme.backend.entity.TripStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<TripEntity, UUID> {
    List<TripEntity> findAllByPassengerId(UUID passengerId);
    List<TripEntity> findAllByDriverId(UUID driverId);
    List<TripEntity> findAllByStatus(TripStatusEnum status);
}

