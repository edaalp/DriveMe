package com.driveme.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageDTO {
    private String rideId;
    private String senderId;
    private String senderRole;
    private String content;
}