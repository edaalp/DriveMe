package com.driveme.backend.repository;

import com.driveme.backend.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

    Optional<VehicleDocument> findByIdAndVehicle_Id(UUID documentId, UUID vehicleId);
}
