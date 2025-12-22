package com.driveme.requests;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ride_request")
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double pickupLat;
    private Double pickupLng;
    private Double destLat;
    private Double destLng;

    private Instant createdAt = Instant.now();

    protected RideRequest() {}

    public RideRequest(Double pickupLat, Double pickupLng, Double destLat, Double destLng) {
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.destLat = destLat;
        this.destLng = destLng;
    }

    public Long getId() { return id; }
    public Double getPickupLat() { return pickupLat; }
    public Double getPickupLng() { return pickupLng; }
    public Double getDestLat() { return destLat; }
    public Double getDestLng() { return destLng; }
    public Instant getCreatedAt() { return createdAt; }
}
