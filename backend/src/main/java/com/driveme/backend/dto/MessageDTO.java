package com.driveme.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MessageDTO {
    private UUID rideId;
    private UUID senderId;
    private String senderRole;
    private String content;
}