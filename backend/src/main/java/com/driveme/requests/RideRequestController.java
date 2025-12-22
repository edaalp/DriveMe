package com.driveme.requests;

import com.driveme.requests.dto.CreateRideRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
public class RideRequestController {

    private final RideRequestService service;

    public RideRequestController(RideRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid CreateRideRequestDto dto) {
        var created = service.createAndBroadcast(dto);
        return ResponseEntity.ok().body(
                java.util.Map.of("id", created.getId())
        );
    }
}
