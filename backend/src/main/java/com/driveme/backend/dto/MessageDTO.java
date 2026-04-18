package com.driveme.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageDTO {
    private Long rideId;
    private Long senderId;
    private String senderRole;
    private String content;
}