package com.driveme.backend.repository;

import com.driveme.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRideIdOrderBySentAtAsc(Long rideId);
}