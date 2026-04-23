package com.driveme.backend.helper;

import com.driveme.backend.entity.Passenger;
import com.driveme.backend.dto.PassengerDTO;
import com.driveme.backend.auth.PassengerSignUpRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Passenger entities and DTOs.
 */
@Component
public class PassengerMapper {

    /**
     * Convert sign-up request DTO to Passenger entity (no document URLs yet).
     */
    public Passenger toEntity(PassengerSignUpRequest request, String hashedPassword) {
        return toEntity(request, hashedPassword, null, null, null);
    }

    /**
     * Convert sign-up request DTO to Passenger entity with optional document URLs.
     *
     * @param request           the sign-up request
     * @param hashedPassword    bcrypt-hashed password
     * @param profilePictureUrl URL of the selfie (nullable)
     * @param tcPhotoFrontUrl   URL of TC front photo (nullable)
     * @param tcPhotoBackUrl    URL of TC back photo (nullable)
     */
    public Passenger toEntity(
            PassengerSignUpRequest request,
            String hashedPassword,
            String profilePictureUrl,
            String tcPhotoFrontUrl,
            String tcPhotoBackUrl) {
        Passenger passenger = new Passenger();
        passenger.setEmail(request.getEmail());
        passenger.setFullName(request.getFullName());
        passenger.setPhoneNumber(request.getPhoneNumber());
        passenger.setPasswordHash(hashedPassword);
        passenger.setActive(true);
        passenger.setTcNo(request.getTcNo());
        passenger.setProfilePictureUrl(profilePictureUrl);
        passenger.setTcPhotoFrontUrl(tcPhotoFrontUrl);
        passenger.setTcPhotoBackUrl(tcPhotoBackUrl);
        return passenger;
    }

    /**
     * Convert Passenger entity to response DTO.
     * Excludes sensitive information like password hash.
     */
    public PassengerDTO toDTO(Passenger passenger) {
        if (passenger == null) {
            return null;
        }

        PassengerDTO response = new PassengerDTO();
        response.setId(passenger.getId());
        response.setEmail(passenger.getEmail());
        response.setFullName(passenger.getFullName());
        response.setPhoneNumber(passenger.getPhoneNumber());
        response.setActive(passenger.isActive());
        response.setTcNo(passenger.getTcNo());
        response.setTcPhotoFrontUrl(passenger.getTcPhotoFrontUrl());
        response.setTcPhotoBackUrl(passenger.getTcPhotoBackUrl());
        response.setProfilePictureUrl(passenger.getProfilePictureUrl());
        response.setVerificationStatus(passenger.getVerificationStatus());
        response.setRejectionReason(passenger.getRejectionReason());
        return response;
    }
}
