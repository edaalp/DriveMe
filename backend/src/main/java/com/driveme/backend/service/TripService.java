package com.driveme.backend.service;

import com.driveme.backend.dto.TripDTO;
import com.driveme.backend.entity.TripEntity;
import com.driveme.backend.entity.TripStatusEnum;
import com.driveme.backend.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {
    private final TripRepository tripRepository;

    public TripEntity findById(UUID id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
    }

    public List<TripEntity> findByPassengerId(UUID passengerId) {
        log.info("Fetching trips for passenger: {}", passengerId);
        return tripRepository.findAllByPassengerId(passengerId);
    }

    public List<TripEntity> findByDriverId(UUID driverId) {
        log.info("Fetching trips for driver: {}", driverId);
        return tripRepository.findAllByDriverId(driverId);
    }

    public TripEntity createTrip(TripDTO trip) {
        log.info("Creating new trip");
        TripEntity tripEntity = new TripEntity();
        tripEntity.setPassengerId(trip.getPassengerId());
        tripEntity.setDriverId(trip.getDriverId());
        tripEntity.setStatus(TripStatusEnum.valueOf(trip.getStatus()));
        try {
            return tripRepository.save(tripEntity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TripEntity updateTrip(UUID id, TripEntity tripDetails) {
        TripEntity trip = findById(id);
        trip.setStatus(tripDetails.getStatus());
        log.info("Updating trip: {}", id);
        return tripRepository.save(trip);
    }

    public boolean tripExists(UUID id) {
        return tripRepository.existsById(id);
    }
}
