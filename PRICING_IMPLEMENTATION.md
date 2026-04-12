# DriveMe Backend Pricing System - Implementation Guide

## Overview

A complete, secure, backend-driven pricing system has been implemented for the DriveMe ride-hailing app. The system ensures all price calculations are performed server-side and are consistent across endpoints.

## Architecture

### Core Design Principle
**Single Source of Truth**: All pricing calculations flow through the centralized `PricingService`, ensuring consistency between the price preview endpoint and the official price persisted on trip creation.

### Key Components

#### 1. **PricingService** (`service/pricing/PricingService.java`)
The central pricing engine that orchestrates all pricing calculations:
- Calls `RoutingService` to get distance and duration
- Applies surge multiplier via `DemandSupplyService`
- Applies time-based multipliers via `TimePricingService`
- Applies weather multipliers via `WeatherPricingService`
- Calculates final price range (min: base × 0.9, max: base × 1.1)

**Pricing Formula:**
```
basePrice = baseFare + (distanceKm × perKmRate) + (durationMin × perMinuteRate)
finalBasePrice = basePrice × surgeMultiplier × timeMultiplier × weatherMultiplier
minPrice = finalBasePrice × 0.9
maxPrice = finalBasePrice × 1.1
```

#### 2. **RoutingService** (`service/pricing/RoutingService.java`)
Calculates distance and duration between two locations:
- **Primary**: Google Maps Distance Matrix API (if enabled)
- **Fallback**: Haversine formula with estimated speed (40 km/h)
- Converts distances to km and durations to minutes
- Robust error handling with graceful fallback

#### 3. **DemandSupplyService** (`service/pricing/DemandSupplyService.java`)
Calculates surge pricing based on demand/supply ratio:
- **Current**: Stub implementation returning 1.0 (no surge)
- **Formula**: `surge = min(3.0, 1.0 + 0.5 × log(demand/max(supply, 1)))`
- **Future**: Ready to integrate real demand/supply data

#### 4. **TimePricingService** (`service/pricing/TimePricingService.java`)
Applies time-based pricing multipliers:
- **Rush Hour** (07:00-10:00, 17:00-20:00): 1.2× multiplier
- **Night Time** (22:00-06:00): 1.1× multiplier
- **Normal**: 1.0× multiplier (no change)

#### 5. **WeatherPricingService** (`service/pricing/WeatherPricingService.java`)
Applies weather-based pricing multipliers:
- **Current**: Stub implementation returning 1.0 (no weather surge)
- **Future**: Can integrate weather APIs (OpenWeather, etc.)

#### 6. **PricingConfig** (`config/PricingConfig.java`)
Configuration properties for pricing parameters:
- `baseFare`: 20.0 TRY
- `perKmRate`: 8.0 TRY/km
- `perMinuteRate`: 1.0 TRY/min
- `maxSurgeMultiplier`: 3.0
- `rushHourMultiplier`, `nightTimeMultiplier`, etc.
- Google Maps configuration
- All configurable via `application.yaml` or environment variables

## API Endpoints

### 1. Price Preview Endpoint
**Endpoint**: `POST /api/trip-requests/calculate-price`
**Auth**: Required (Bearer JWT)
**Purpose**: Non-binding price estimate shown to the user before trip creation

**Request Body** (from Flutter):
```json
{
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
}
```

**Response Body** (Flutter's `TripPricePreview` model):
```json
{
  "minPrice": 45.0,
  "maxPrice": 55.0,
  "currency": "TRY",
  "distanceKm": 25.5,
  "durationMinutes": 38
}
```

**Error Response**:
```json
{
  "message": "Failed to calculate price: Invalid coordinates",
  "errorId": "VALIDATION_ERROR",
  "timestamp": 1712876400000
}
```

### 2. Trip Request Creation Endpoint
**Endpoint**: `POST /api/trip-requests`
**Auth**: Required (Bearer JWT)
**Purpose**: Create a trip request with official pricing

**Request Body** (from Flutter):
```json
{
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
}
```

**Response Body** (Flutter's `TripRequest` model):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "requestedTime": "2026-04-11T14:30:00Z",
  "withPet": false,
  "status": "PENDING",
  "minPriceAmount": 45.0,
  "minPriceCurrency": "TRY",
  "maxPriceAmount": 55.0,
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

## Security

### Key Security Features

1. **Backend-Driven Pricing**
   - Client sends ONLY coordinates and trip info
   - Backend NEVER trusts any price data from client
   - All pricing computed server-side

2. **Authentication Required**
   - Both endpoints require valid JWT token
   - Token provides passenger ID
   - PassengerId extracted from JWT, not from request body

3. **Validation**
   - Coordinates validated (lat: -90 to 90, lon: -180 to 180)
   - Locations must be at least 0.05 km apart
   - Pickup and destination both required

4. **Ownership Verification**
   - Vehicle must belong to requesting passenger
   - Only passenger can create trip requests

## Configuration

### application.yaml
```yaml
pricing:
  baseFare: 20.0
  perKmRate: 8.0
  perMinuteRate: 1.0
  maxSurgeMultiplier: 3.0
  currency: TRY
  minPricePercentage: 0.9
  maxPricePercentage: 1.1
  useGoogleMaps: false
  googleMapsApiKey: ${GOOGLE_MAPS_API_KEY:}
  rushHourStartMorning: 7
  rushHourEndMorning: 10
  rushHourStartEvening: 17
  rushHourEndEvening: 20
  rushHourMultiplier: 1.2
  nightTimeStart: 22
  nightTimeEnd: 6
  nightTimeMultiplier: 1.1
```

### Environment Variables
All pricing parameters can be overridden via environment variables:
```bash
export PRICING_BASE_FARE=25.0
export PRICING_PER_KM_RATE=10.0
export PRICING_PER_MINUTE_RATE=1.5
export GOOGLE_MAPS_API_KEY="your-api-key-here"
export PRICING_USE_GOOGLE_MAPS=true
```

## Integration with Existing Code

### Modified Files

1. **TripRequestController** (`controller/TripRequestController.java`)
   - Removed duplicate price calculation endpoints
   - Now delegates to TripPricingController

2. **TripRequestService** (`service/TripRequestService.java`)
   - Added PricingService dependency
   - Updated createTripRequest() to use PricingService
   - Prices now calculated at time of trip creation

3. **application.yaml**
   - Added complete pricing configuration

### pom.xml Dependencies Added
```xml
<dependency>
    <groupId>com.google.maps</groupId>
    <artifactId>google-maps-services</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.32</version>
</dependency>
```

## New Files Created

1. **DTOs**
   - `dto/CalculatePriceRequest.java` - Request for price preview
   - `dto/TripPricePreview.java` - Price preview response
   - `dto/PricingResult.java` - Internal pricing calculation result
   - `dto/ErrorResponse.java` - Standard error response

2. **Configuration**
   - `config/PricingConfig.java` - Pricing parameters configuration

3. **Pricing Services**
   - `service/pricing/PricingService.java` - Main pricing engine
   - `service/pricing/RoutingService.java` - Distance/duration calculation
   - `service/pricing/DemandSupplyService.java` - Surge pricing
   - `service/pricing/TimePricingService.java` - Time-based multipliers
   - `service/pricing/WeatherPricingService.java` - Weather multipliers (stub)

4. **Controllers**
   - `controller/TripPricingController.java` - Price calculation endpoints

## Data Flow

### Price Preview Flow
```
Flutter App
    ↓
POST /api/trip-requests/calculate-price
    ↓
TripPricingController.calculatePrice()
    ↓
PricingService.calculatePrice()
    ├→ RoutingService.calculateRoute() [Google Maps or Haversine]
    ├→ DemandSupplyService.calculateSurgeMultiplier()
    ├→ TimePricingService.calculateTimeMultiplier()
    └→ WeatherPricingService.calculateWeatherMultiplier()
    ↓
TripPricePreview JSON Response
    ↓
Flutter App displays price range to user
```

### Trip Creation Flow
```
Flutter App
    ↓
POST /api/trip-requests (with coordinates, NOT prices)
    ↓
TripRequestController.createTripRequest()
    ↓
TripRequestService.createTripRequest()
    ↓
PricingService.calculatePrice() [Same logic as preview!]
    ├→ RoutingService.calculateRoute()
    ├→ DemandSupplyService.calculateSurgeMultiplier()
    ├→ TimePricingService.calculateTimeMultiplier()
    └→ WeatherPricingService.calculateWeatherMultiplier()
    ↓
Save TripRequest with calculated prices to database
    ↓
TripRequest JSON Response
    ↓
Flutter App receives confirmation with official prices
```

## Future Enhancements

### Phase 2: Real Demand/Supply Data
- Query database for active trip requests in area (geospatial queries)
- Query driver availability in area
- Update DemandSupplyService with real calculations

### Phase 3: Weather Integration
- Integrate OpenWeather API
- Calculate weather multiplier based on conditions
- Cache weather data to avoid excessive API calls

### Phase 4: Machine Learning
- Collect historical trip data
- Train ML model for dynamic pricing
- Gradually replace formula-based pricing

### Phase 5: Loyalty & Promotions
- Store passenger loyalty tier
- Apply discount multipliers
- Support promotional codes

## Testing

### Manual Testing with cURL

```bash
# Set your JWT token
TOKEN="your-jwt-token-here"

# Test price preview
curl -X POST http://10.0.2.2:8080/api/trip-requests/calculate-price \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "pickup": {"lat": 41.0082, "lon": 28.9784, "addressText": "Istanbul Airport"},
    "destination": {"lat": 41.0096, "lon": 29.1684, "addressText": "Downtown Istanbul"}
  }'

# Test trip creation
curl -X POST http://10.0.2.2:8080/api/trip-requests \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requestedTime": "2026-04-11T14:30:00Z",
    "withPet": false,
    "pickup": {"lat": 41.0082, "lon": 28.9784, "addressText": "Istanbul Airport"},
    "destination": {"lat": 41.0096, "lon": 29.1684, "addressText": "Downtown Istanbul"},
    "vehicleId": null
  }'
```

## Troubleshooting

### Issue: Prices not calculated
**Solution**: Check that PricingService is properly autowired in TripRequestService

### Issue: Google Maps API errors
**Solution**: 
1. Set `pricing.useGoogleMaps: false` to use Haversine fallback
2. Or provide valid Google Maps API key via `GOOGLE_MAPS_API_KEY` environment variable

### Issue: Rush hour/night multipliers not applying
**Solution**: Check server timezone is set to Istanbul (`Europe/Istanbul`)

## Performance Considerations

1. **Distance Calculation**: 
   - Haversine formula is O(1) and very fast
   - Google Maps API adds ~200-500ms latency

2. **Caching Opportunities**:
   - Cache demand/supply data (refresh every 5-10 minutes)
   - Cache weather data (refresh every 30 minutes)

3. **Async Improvements**:
   - Make Google Maps calls async in future
   - Batch distance calculations if needed

---

**Implementation Date**: April 11, 2026
**Version**: 1.0

