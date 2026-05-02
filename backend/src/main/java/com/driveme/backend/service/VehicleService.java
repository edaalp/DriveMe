package com.driveme.backend.service;

import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.common.VehicleDocumentType;
import com.driveme.backend.dto.CreateVehicleRequest;
import com.driveme.backend.dto.VehicleDTO;
import com.driveme.backend.entity.Passenger;
import com.driveme.backend.entity.Vehicle;
import com.driveme.backend.entity.VehicleDocument;
import com.driveme.backend.helper.VehicleMapper;
import com.driveme.backend.repository.PassengerRepository;
import com.driveme.backend.repository.VehicleDocumentRepository;
import com.driveme.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Vehicle operations.
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final PassengerRepository passengerRepository;
    private final EmailService emailService;

    /**
     * Get all vehicles for a passenger.
     */
    @Transactional(readOnly = true)
    public List<VehicleDTO> getVehiclesByPassengerId(UUID passengerId) {
        return vehicleRepository.findByPassengerIdWithDocuments(passengerId)
                .stream()
                .map(VehicleMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a vehicle by ID.
     */
    @Transactional(readOnly = true)
    public VehicleDTO getVehicleById(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdWithDocuments(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));
        return VehicleMapper.toDTO(vehicle);
    }

    /**
     * Create a new vehicle for a passenger.
     */
    @Transactional
    public VehicleDTO createVehicle(UUID passengerId, CreateVehicleRequest request) {
        if (vehicleRepository.existsByPlateNumber(request.getPlateNumber())) {
            throw new RuntimeException("A vehicle with this plate number already exists");
        }

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));

        Vehicle vehicle = VehicleMapper.toEntity(request, passenger);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return VehicleMapper.toDTO(vehicleRepository.findByIdWithDocuments(savedVehicle.getId())
                .orElse(savedVehicle));
    }

    /**
     * Delete a vehicle.
     */
    @Transactional
    public void deleteVehicle(UUID vehicleId, UUID passengerId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        if (!vehicle.getPassenger().getId().equals(passengerId)) {
            throw new RuntimeException("You don't have permission to delete this vehicle");
        }

        vehicleRepository.delete(vehicle);
    }

    /**
     * Upload one or more documents / photos for a vehicle.
     * [documentTypes] CSV order must align with [files] (same length).
     */
    @Transactional
    public VehicleDTO uploadDocuments(
            UUID vehicleId,
            UUID passengerId,
            List<MultipartFile> files,
            String documentTypesCsv) throws IOException {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }
        if (documentTypesCsv == null || documentTypesCsv.isBlank()) {
            throw new IllegalArgumentException("documentTypes is required");
        }

        String[] typeTokens = documentTypesCsv.split(",");
        if (typeTokens.length != files.size()) {
            throw new IllegalArgumentException("documentTypes count must match files count");
        }

        List<VehicleDocumentType> types = new ArrayList<>(typeTokens.length);
        for (String token : typeTokens) {
            String t = token.trim();
            if (t.isEmpty()) {
                throw new IllegalArgumentException("Empty document type in documentTypes");
            }
            try {
                types.add(VehicleDocumentType.valueOf(t));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid document type: " + t);
            }
        }

        Vehicle vehicle = vehicleRepository.findByIdWithDocuments(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        if (!vehicle.getPassenger().getId().equals(passengerId)) {
            throw new RuntimeException("You don't have permission to upload documents for this vehicle");
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile f = files.get(i);
            if (f == null || f.isEmpty()) {
                continue;
            }
            VehicleDocument doc = new VehicleDocument();
            doc.setVehicle(vehicle);
            doc.setFileName(f.getOriginalFilename());
            doc.setDocumentType(types.get(i));
            doc.setFileContent(f.getBytes());
            vehicle.getDocuments().add(doc);
        }

        vehicleRepository.save(vehicle);
        return VehicleMapper.toDTO(vehicleRepository.findByIdWithDocuments(vehicleId)
                .orElse(vehicle));
    }

    /**
     * Check if a vehicle belongs to a passenger.
     */
    @Transactional(readOnly = true)
    public boolean isVehicleOwnedByPassenger(UUID vehicleId, UUID passengerId) {
        return vehicleRepository.findById(vehicleId)
                .map(v -> v.getPassenger().getId().equals(passengerId))
                .orElse(false);
    }

    // ---- Admin verification methods ----

    @Transactional(readOnly = true)
    public List<VehicleDTO> getVehiclesByStatus(VerificationStatus status) {
        return vehicleRepository.findByStatusWithDocuments(status)
                .stream()
                .map(VehicleMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleDTO> getAllVehicles() {
        return vehicleRepository.findAllWithDocuments()
                .stream()
                .map(VehicleMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehicleDTO verifyVehicle(UUID vehicleId, VerificationStatus decision, String reason) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        if (decision == VerificationStatus.PENDING) {
            throw new IllegalArgumentException("Decision must be VERIFIED or REJECTED");
        }

        VerificationStatus previousStatus = vehicle.getStatus();

        vehicle.setStatus(decision);
        vehicle.setRejectionReason(decision == VerificationStatus.REJECTED ? reason : null);

        Vehicle saved = vehicleRepository.save(vehicle);

        if (decision == VerificationStatus.VERIFIED
                && previousStatus != VerificationStatus.VERIFIED) {
            Passenger owner = saved.getPassenger();
            if (owner != null) {
                String vehicleLabel = buildVehicleLabel(saved);
                emailService.sendVehicleApprovedNotification(
                        owner.getEmail(),
                        owner.getFullName(),
                        vehicleLabel,
                        saved.getPlateNumber());
            }
        }

        return VehicleMapper.toDTO(vehicleRepository.findByIdWithDocuments(vehicleId)
                .orElse(saved));
    }

    /**
     * Human-readable vehicle description for notification emails,
     * e.g. {@code "Toyota Corolla (2021)"}. Falls back gracefully when
     * individual fields are null.
     */
    private static String buildVehicleLabel(Vehicle vehicle) {
        String brand = vehicle.getBrand() == null ? "" : vehicle.getBrand().trim();
        String model = vehicle.getModel() == null ? "" : vehicle.getModel().trim();
        Integer year = vehicle.getYear();

        String base = (brand + " " + model).trim();
        if (base.isEmpty()) {
            base = "Your vehicle";
        }
        return year != null ? base + " (" + year + ")" : base;
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicleEntityById(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));
    }

    /**
     * Resolve a document for admin preview/download (scoped to vehicle).
     */
    @Transactional(readOnly = true)
    public VehicleDocument getDocumentForAdmin(UUID vehicleId, UUID documentId) {
        return vehicleDocumentRepository.findByIdAndVehicle_Id(documentId, vehicleId)
                .orElseThrow(() -> new RuntimeException("Document not found for this vehicle"));
    }
}
