# DriveMe Pricing API - Quick Reference

## Endpoints

### 1. Price Preview (Non-binding Estimate)
**Endpoint**: `POST /api/trip-requests/calculate-price`  
**Auth**: Required (Bearer JWT)  
**Purpose**: Show user estimated price range before trip creation

#### Request
```bash
curl -X POST http://10.0.2.2:8080/api/trip-requests/calculate-price \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "pickup": {
      "lat": 41.0082,
      "lon": 28.9784,
      "addressText": "Istanbul Airport"
    },
    "destination": {
      "lat": 41.0096,
      "lon": 29.1684,
      "addressText": "Downtown Istanbul"
    }
  }'
```

#### Success Response (HTTP 200)
```json
{
  "minPrice": 278.64,
  "maxPrice": 340.56,
  "currency": "TRY",
  "distanceKm": 25.5,
  "durationMinutes": 38
}
```

#### Error Response (HTTP 400/500)
```json
{
  "message": "Pickup and destination cannot be the same location",
  "errorId": "VALIDATION_ERROR",
  "timestamp": 1712876400000
}
```

---

### 2. Create Trip Request (Binding)
**Endpoint**: `POST /api/trip-requests`  
**Auth**: Required (Bearer JWT)  
**Purpose**: Create trip request with official pricing

#### Request
```bash
curl -X POST http://10.0.2.2:8080/api/trip-requests \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "requestedTime": "2026-04-11T14:30:00Z",
    "withPet": false,
    "pickup": {
      "lat": 41.0082,
      "lon": 28.9784,
      "addressText": "Istanbul Airport"
    },
    "destination": {
      "lat": 41.0096,
      "lon": 29.1684,
      "addressText": "Downtown Istanbul"
    },
    "vehicleId": null
  }'
```

#### Success Response (HTTP 201)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "requestedTime": "2026-04-11T14:30:00Z",
  "withPet": false,
  "status": "PENDING",
  "minPriceAmount": 278.64,
  "minPriceCurrency": "TRY",
  "maxPriceAmount": 340.56,
  "maxPriceCurrency": "TRY",
  "pickup": {
    "lat": 41.0082,
    "lon": 28.9784,
    "addressText": "Istanbul Airport"
  },
  "destination": {
    "lat": 41.0096,
    "lon": 29.1684,
    "addressText": "Downtown Istanbul"
  },
  "distanceKm": 25.5,
  "passengerId": "550e8400-e29b-41d4-a716-446655440001",
  "vehicle": null,
  "createdAt": "2026-04-11T14:25:30Z",
  "updatedAt": "2026-04-11T14:25:30Z"
}
```

---

## Request Validation Rules

### Location Validation
| Field | Type | Range | Required |
|-------|------|-------|----------|
| `lat` | Double | -90 to 90 | Yes |
| `lon` | Double | -180 to 180 | Yes |
| `addressText` | String | Max 500 chars | No |

### Trip Validation
- Pickup and destination must be at least 0.05 km apart
- Both coordinates must be valid
- Requested time must be in future (ISO-8601 UTC format)

---

## Price Calculation Examples

### Example 1: Short Urban Trip (No Surge)
```
Distance: 5 km
Duration: 12 minutes
Time: 14:00 (normal time)

Calculation:
basePrice = 20 + (5 × 8) + (12 × 1) = 20 + 40 + 12 = 72 TRY
Multipliers: surge=1.0, time=1.0, weather=1.0
finalBasePrice = 72 × 1.0 × 1.0 × 1.0 = 72 TRY
minPrice = 72 × 0.9 = 64.80 TRY
maxPrice = 72 × 1.1 = 79.20 TRY

Response: {
  "minPrice": 64.80,
  "maxPrice": 79.20,
  "currency": "TRY",
  "distanceKm": 5.0,
  "durationMinutes": 12
}
```

### Example 2: Long Trip During Rush Hour
```
Distance: 30 km
Duration: 45 minutes
Time: 08:00 (rush hour)

Calculation:
basePrice = 20 + (30 × 8) + (45 × 1) = 20 + 240 + 45 = 305 TRY
Multipliers: surge=1.0, time=1.2 (rush hour), weather=1.0
finalBasePrice = 305 × 1.0 × 1.2 × 1.0 = 366 TRY
minPrice = 366 × 0.9 = 329.40 TRY
maxPrice = 366 × 1.1 = 402.60 TRY

Response: {
  "minPrice": 329.40,
  "maxPrice": 402.60,
  "currency": "TRY",
  "distanceKm": 30.0,
  "durationMinutes": 45
}
```

### Example 3: Late Night Trip
```
Distance: 10 km
Duration: 18 minutes
Time: 23:00 (night time)

Calculation:
basePrice = 20 + (10 × 8) + (18 × 1) = 20 + 80 + 18 = 118 TRY
Multipliers: surge=1.0, time=1.1 (night), weather=1.0
finalBasePrice = 118 × 1.0 × 1.1 × 1.0 = 129.80 TRY
minPrice = 129.80 × 0.9 = 116.82 TRY
maxPrice = 129.80 × 1.1 = 142.78 TRY

Response: {
  "minPrice": 116.82,
  "maxPrice": 142.78,
  "currency": "TRY",
  "distanceKm": 10.0,
  "durationMinutes": 18
}
```

---

## HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | Price calculated successfully | Normal price response |
| 201 | Trip request created successfully | Trip creation response |
| 400 | Bad request (validation error) | Invalid coordinates, same location |
| 401 | Unauthorized (missing/invalid JWT) | No Authorization header |
| 403 | Forbidden (not passenger user) | Driver trying to create trip |
| 500 | Server error | Routing API failure, database error |

---

## Error Codes

| ErrorId | Meaning | HTTP Status |
|---------|---------|-------------|
| VALIDATION_ERROR | Input validation failed | 400 |
| PRICING_ERROR | Price calculation failed | 500 |
| INTERNAL_ERROR | Unexpected server error | 500 |
| AUTH_ERROR | Authentication failed | 401 |
| PERMISSION_ERROR | User lacks permission | 403 |

---

## Configuration

### Default Pricing Parameters
```yaml
baseFare: 20.0 TRY           # Flat fee per trip
perKmRate: 8.0 TRY/km        # Distance charge
perMinuteRate: 1.0 TRY/min   # Time charge
maxSurgeMultiplier: 3.0x     # Max surge cap
```

### Time-Based Multipliers
```
Normal Time: 1.0x
Rush Hour (07:00-10:00, 17:00-20:00): 1.2x
Night Time (22:00-06:00): 1.1x
```

### Override with Environment Variables
```bash
export PRICING_BASE_FARE=25.0
export PRICING_PER_KM_RATE=10.0
export PRICING_PER_MINUTE_RATE=1.5
export PRICING_RUSH_HOUR_MULTIPLIER=1.3
export PRICING_NIGHT_TIME_MULTIPLIER=1.2
```

---

## Important Notes

### ⚠️ Security Guarantees
- **Backend-driven**: All prices calculated on server, never trusted from client
- **Consistent**: Preview and creation use identical formula
- **Authenticated**: Both endpoints require valid JWT
- **Validated**: All inputs validated server-side

### ℹ️ Distance Calculation
- **Primary**: Google Maps Distance Matrix API (if enabled)
- **Fallback**: Haversine formula with 40 km/h average speed
- **Both** return distance in km and duration in minutes

### 💡 Tips for Clients
1. Call `/calculate-price` FIRST to show user estimate
2. User reviews price and taps "Request"
3. Your app then calls `/trip-requests` with same coordinates
4. Final price will be very close to preview (usually identical if same time)
5. Store trip ID and prices in your local database

---

## Testing with Insomnia/Postman

### Setup
1. Login to get JWT token
2. Set `Authorization: Bearer <token>` header
3. Use `http://10.0.2.2:8080` (Android emulator) or `http://localhost:8080` (direct)

### Test Script
```bash
#!/bin/bash

TOKEN="your-jwt-token-here"
BASE_URL="http://10.0.2.2:8080"

# 1. Get price estimate
echo "=== Getting price estimate ==="
curl -X POST $BASE_URL/api/trip-requests/calculate-price \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "pickup": {"lat": 41.0, "lon": 28.9, "addressText": "A"},
    "destination": {"lat": 41.1, "lon": 29.2, "addressText": "B"}
  }' | jq .

# 2. Create trip
echo ""
echo "=== Creating trip request ==="
curl -X POST $BASE_URL/api/trip-requests \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requestedTime": "2026-04-11T14:30:00Z",
    "withPet": false,
    "pickup": {"lat": 41.0, "lon": 28.9, "addressText": "A"},
    "destination": {"lat": 41.1, "lon": 29.2, "addressText": "B"},
    "vehicleId": null
  }' | jq .
```

---

## Troubleshooting

### "Pickup and destination cannot be the same location"
- Coordinates are too close (< 0.05 km)
- Use different locations at least 50 meters apart

### "Passenger not found"
- JWT token may be invalid or expired
- User ID in token doesn't exist in database
- Try logging in again

### "Failed to calculate price: Invalid coordinates"
- Latitude not in range -90 to 90
- Longitude not in range -180 to 180
- Check your coordinate values

### "Internal server error" (500)
- Routing API may be down
- Check server logs
- System will fallback to Haversine formula automatically

---

## See Also
- [Pricing Implementation Guide](./PRICING_IMPLEMENTATION.md)
- [Implementation Summary](./IMPLEMENTATION_SUMMARY.md)

