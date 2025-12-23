package com.driveme.backend.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    
    private String userType; // DRIVER or PASSENGER
    
    private String userId;
    
    private String email;
    
    private String fullName;
    
    private String userName;
}
