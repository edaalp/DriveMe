package com.driveme.backend.user;

import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Passenger entities and DTOs.
 */
@Component
public class PassengerMapper {

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
        return passenger;
    }

    /**
     * Convert Passenger entity to response DTO.
     * Excludes sensitive information like password hash.
     * 
     * @param passenger the passenger entity
     * @return the passenger response DTO
     */
    public PassengerResponse toResponse(Passenger passenger) {
        if (passenger == null) {
            return null;
        }
        
        PassengerResponse response = new PassengerResponse();
        response.setId(passenger.getId());
        response.setEmail(passenger.getEmail());
        response.setFullName(passenger.getFullName());
        response.setPhoneNumber(passenger.getPhoneNumber());
        response.setActive(passenger.isActive());
        return response;
    }
}
