package com.driveme.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@RequiredArgsConstructor
public class RatingEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    @Column(nullable=false, updatable= false)
    private UUID ratingId;

    @Column(nullable = false)
    private UUID raterId;

    @Column(nullable = false)
    private UUID rateeId;

    @Column(nullable = false)
    private int score;

    @Column
    private String comment;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

}
