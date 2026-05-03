package com.driveme.backend.entity;

import com.driveme.backend.common.BaseEntity;
import com.driveme.backend.common.TransmissionType;
import com.driveme.backend.common.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Vehicle entity representing a passenger's vehicle.
 */
@Entity
@Table(name = "vehicles")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String plateNumber;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransmissionType transmission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] documentFile;

    private String documentFileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;
}

