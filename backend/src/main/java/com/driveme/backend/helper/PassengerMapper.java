package com.driveme.backend.helper;

import com.driveme.backend.auth.PassengerSignUpRequest;
import com.driveme.backend.common.VerificationStatus;
import com.driveme.backend.dto.PassengerAdminResponse;
import com.driveme.backend.dto.PassengerDTO;
import com.driveme.backend.entity.Passenger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Passenger entities and DTOs.
 */
@Component
public class PassengerMapper {

    @Value("${app.public-base-url:http://10.0.2.2:8080}")
    private String publicBaseUrl;

    /**
     * Convert sign-up request DTO to Passenger entity.
     * 
     * @param request the sign-up request
     * @param hashedPassword the hashed password
     * @return the passenger entity
     */
    public Passenger toEntity(PassengerSignUpRequest request, String hashedPassword) {
        Passenger passenger = new Passenger();
        passenger.setEmail(request.getEmail());
        passenger.setFullName(request.getFullName());
        passenger.setPhoneNumber(request.getPhoneNumber());
        passenger.setPasswordHash(hashedPassword);
        passenger.setActive(true);
        passenger.setVerificationStatus(VerificationStatus.PENDING);
        return passenger;
    }

    /**
     * Convert Passenger entity to response DTO.
     * Excludes sensitive information like password hash.
     * 
     * @param passenger the passenger entity
     * @return the passenger response DTO
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
        return response;
    }

    /**
     * Admin list/detail: public URLs for document endpoints (same pattern as {@link DriverMapper}).
     */
    public PassengerAdminResponse toAdminResponse(Passenger passenger) {
        if (passenger == null) {
            return null;
        }
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        String idStr = passenger.getId().toString();

        VerificationStatus status = passenger.getVerificationStatus();
        if (status == null) {
            status = VerificationStatus.PENDING;
        }

        PassengerAdminResponse response = new PassengerAdminResponse();
        response.setId(passenger.getId());
        response.setEmail(passenger.getEmail());
        response.setFullName(passenger.getFullName());
        response.setPhoneNumber(passenger.getPhoneNumber());
        response.setUserName(passenger.getUserName());
        response.setActive(passenger.isActive());
        response.setIdentityDocumentFileName(passenger.getIdentityDocumentFileName());
        response.setVerificationStatus(status);
        response.setRejectionReason(passenger.getRejectionReason());

        response.setProfilePictureUrl(
                passenger.getProfilePictureFile() != null && passenger.getProfilePictureFile().length > 0
                        ? base + "/api/admin/passengers/" + idStr + "/document/profile-picture"
                        : null);
        response.setIdentityDocumentUrl(
                passenger.getIdentityDocumentFile() != null && passenger.getIdentityDocumentFile().length > 0
                        ? base + "/api/admin/passengers/" + idStr + "/document/identity-document"
                        : null);

        return response;
    }
}
