package com.driveme.backend.controller;

import com.driveme.backend.dto.TripDTO;
import com.driveme.backend.entity.TripEntity;
import com.driveme.backend.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {
    private final TripService tripService;

    @GetMapping("/{id}")
    public ResponseEntity<TripEntity> getTripById(@PathVariable UUID id) {
        log.info("GET /api/trips/{}", id);
        return ResponseEntity.ok(tripService.findById(id));
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<TripEntity>> getPassengerTrips(
            @PathVariable UUID passengerId) {
        log.info("GET /api/trips/passenger/{}", passengerId);
        return ResponseEntity.ok(tripService.findByPassengerId(passengerId));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<TripEntity>> getDriverTrips(
            @PathVariable UUID driverId) {
        log.info("GET /api/trips/driver/{}", driverId);
        return ResponseEntity.ok(tripService.findByDriverId(driverId));
    }

    @PostMapping
    public ResponseEntity<TripEntity> createTrip(@RequestBody TripDTO trip) {
        log.info("Creating trip: {}", trip.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTrip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripEntity> updateTrip(
            @PathVariable UUID id,
            @RequestBody TripEntity tripDetails) {
        log.info("PUT /api/trips/{}", id);
        return ResponseEntity.ok(tripService.updateTrip(id, tripDetails));
    }

}
