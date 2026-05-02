package com.driveme.backend.entity;

import com.driveme.backend.common.BaseEntity;
import com.driveme.backend.common.VehicleDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * Binary document or photo attached to a vehicle (Neon: BYTEA via {@link Types#BINARY} mapping).
 */
@Entity
@Table(name = "vehicle_documents")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(length = 512)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VehicleDocumentType documentType;

    @JdbcTypeCode(Types.BINARY)
    @Column(name = "file_content", nullable = false, columnDefinition = "bytea")
    private byte[] fileContent;
}
