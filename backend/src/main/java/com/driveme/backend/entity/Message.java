package com.driveme.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "ride_id", columnDefinition = "uuid")
    private UUID rideId;

    @Column(name = "sender_id", columnDefinition = "uuid")
    private UUID senderId;

    @Column(name = "sender_role", columnDefinition = "uuid")
    private String senderRole;   // "DRIVER" or "PASSENGER"
    private String content;

    @Column(updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

}