package com.driveme.requests;

import com.driveme.requests.dto.CreateRideRequestDto;
import com.driveme.requests.dto.RideRequestEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RideRequestService {

    private final RideRequestRepository repo;
    private final SimpMessagingTemplate messaging;

    public RideRequestService(RideRequestRepository repo, SimpMessagingTemplate messaging) {
        this.repo = repo;
        this.messaging = messaging;
    }

    public RideRequest createAndBroadcast(CreateRideRequestDto dto) {
        var saved = repo.save(new RideRequest(dto.pickupLat(), dto.pickupLng(), dto.destLat(), dto.destLng()));

        // Broadcast to all drivers subscribed to /topic/requests
        var event = new RideRequestEvent(
                saved.getId(),
                saved.getPickupLat(), saved.getPickupLng(),
                saved.getDestLat(), saved.getDestLng(),
                saved.getCreatedAt()
        );

        messaging.convertAndSend("/topic/requests", event);
        return saved;
    }
}
