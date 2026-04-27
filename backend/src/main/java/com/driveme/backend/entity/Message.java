package com.driveme.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private String rideId;
    private String senderId;
    private String senderRole;   // "DRIVER" or "PASSENGER"
    private String content;

    @Column(updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

}