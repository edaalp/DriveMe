# Developer's Guide: Extending the DriveMe Pricing System

## Overview

This guide explains how to extend and customize the pricing system for future enhancements like surge pricing, weather integration, and machine learning.

---

## Architecture Review

The pricing system follows a **layered architecture** with a **single entry point**:

```
Client Request (Flutter)
         ↓
    Controller Layer (TripPricingController)
         ↓
    Service Layer (TripRequestService, PricingService)
         ↓
    Pricing Engine (PricingService + 4 helper services)
         ↓
    Database / External APIs
         ↓
    Response to Client
```

### Key Design Principles

1. **Single Responsibility**: Each service has one clear job
2. **Dependency Injection**: All services use Spring @RequiredArgsConstructor
3. **Configuration Externalization**: Parameters in application.yaml
4. **Error Handling**: Graceful degradation (e.g., Haversine fallback)
5. **Logging**: Debug-level for tracing, info for important events

---

## Extending the System

### 1. Adding Real Demand/Supply Surge Pricing

**Location**: `service/pricing/DemandSupplyService.java`

**Current Implementation** (stub):
```java
private int getActiveDemandInArea(Location location) {
    return 1;  // Always returns 1 (no surge)
}

private int getAvailableSupplyInArea(Location location) {
    return 1;  // Always returns 1 (no surge)
}
```

**How to Implement**:

1. **Add geospatial indexing to database**
   ```sql
   -- In migration SQL file
   CREATE INDEX idx_trip_requests_pickup_geom 
   ON trip_requests USING GIST (ST_GeomFromText('POINT(' || pickup_lon || ' ' || pickup_lat || ')'));
   
   CREATE INDEX idx_drivers_location_geom 
   ON drivers USING GIST (ST_GeomFromText('POINT(' || lon || ' ' || lat || ')'));
   ```

2. **Create custom repository queries**
   ```java
   // In TripRequestRepository
   @Query(value = "SELECT COUNT(*) FROM trip_requests tr " +
                  "WHERE ST_Distance(ST_Point(tr.pickup_lon, tr.pickup_lat), " +
                  "ST_Point(:lon, :lat)) < :radiusKm * 1000 " +
                  "AND tr.status = 'PENDING'", nativeQuery = true)
   int countActiveDemandInArea(@Param("lat") Double lat, 
                               @Param("lon") Double lon,
                               @Param("radiusKm") int radiusKm);
   ```

3. **Implement the service method**
   ```java
   private int getActiveDemandInArea(Location location) {
       // Query all trip requests within 5 km that are still pending
       int demandRadius = 5;  // km
       return tripRequestRepository.countActiveDemandInArea(
           location.getLat(), 
           location.getLon(), 
           demandRadius
       );
   }
   ```

---

### 2. Integrating Weather API

**Location**: `service/pricing/WeatherPricingService.java`

**Current Implementation** (stub):
```java
public double calculateWeatherMultiplier(Location location, Instant time) {
    return 1.0;  // No weather impact
}
```

**How to Implement**:

1. **Add OpenWeather dependency to pom.xml**
   ```xml
   <dependency>
       <groupId>org.json</groupId>
       <artifactId>json</artifactId>
       <version>20230227</version>
   </dependency>
   ```

2. **Create weather service client**
   ```java
   @Service
   @RequiredArgsConstructor
   @Slf4j
   public class WeatherPricingService {
       
       private final RestTemplate restTemplate;
       private final PricingConfig pricingConfig;
       
       @Scheduled(fixedRate = 1800000)  // Cache for 30 minutes
       private Map<String, WeatherData> weatherCache = new ConcurrentHashMap<>();
       
       public double calculateWeatherMultiplier(Location location, Instant time) {
           try {
               String cacheKey = location.getLat() + "," + location.getLon();
               
               WeatherData weather = weatherCache.computeIfAbsent(cacheKey, key -> {
                   return fetchWeatherData(location);
               });
               
               return getMultiplierForWeather(weather);
           } catch (Exception e) {
               log.warn("Error fetching weather data, using default 1.0", e);
               return 1.0;
           }
       }
       
       private WeatherData fetchWeatherData(Location location) {
           String url = String.format(
               "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s",
               location.getLat(), location.getLon(), pricingConfig.getWeatherApiKey()
           );
           
           ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
           JSONObject json = new JSONObject(response.getBody());
           
           return WeatherData.builder()
               .main(json.getJSONArray("weather").getJSONObject(0).getString("main"))
               .temperature(json.getJSONObject("main").getDouble("temp"))
               .build();
       }
       
       private double getMultiplierForWeather(WeatherData weather) {
           switch (weather.getMain().toLowerCase()) {
               case "rain":
                   return 1.3;  // 30% increase
               case "snow":
                   return 1.5;  // 50% increase
               case "fog":
               case "mist":
                   return 1.2;  // 20% increase
               case "thunderstorm":
                   return 1.6;  // 60% increase
               default:
                   return 1.0;  // No change
           }
       }
   }
   ```

---

### 3. Adding Loyalty Discounts

**Location**: Modify `PricingService.calculatePrice()`

**Implementation**:

1. **Add loyalty field to Passenger entity** (if not already present)
   ```java
   @Entity
   public class Passenger extends BaseUser {
       enum LoyaltyTier { BRONZE, SILVER, GOLD, PLATINUM }
       
       @Enumerated(EnumType.STRING)
       private LoyaltyTier loyaltyTier = LoyaltyTier.BRONZE;
   }
   ```

2. **Add loyalty multiplier to PricingService**
   ```java
   public PricingResult calculatePrice(
           Location pickup,
           Location destination,
           Vehicle vehicle,
           boolean withPet,
           Instant requestedTime,
           Passenger passenger) {
       
       // ... existing code ...
       
       // Apply loyalty discount AFTER other multipliers
       double loyaltyMultiplier = calculateLoyaltyMultiplier(passenger);
       double finalBasePrice = basePrice * surgeMultiplier * timeMultiplier 
                             * weatherMultiplier * loyaltyMultiplier;
       
       // ... rest of code ...
   }
   
   private double calculateLoyaltyMultiplier(Passenger passenger) {
       if (passenger == null || passenger.getLoyaltyTier() == null) {
           return 1.0;
       }
       
       switch (passenger.getLoyaltyTier()) {
           case BRONZE:
               return 1.0;   // No discount
           case SILVER:
               return 0.95;  // 5% discount
           case GOLD:
               return 0.90;  // 10% discount
           case PLATINUM:
               return 0.85;  // 15% discount
           default:
               return 1.0;
       }
   }
   ```

---

### 4. Implementing Machine Learning Pricing

**Location**: Create new `service/pricing/MLPricingService.java`

**Implementation**:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MLPricingService {
    
    private final PricingService fallbackPricingService;
    private final MLModel mlModel;  // Your ML model
    
    public double predictPrice(Location pickup, Location destination, 
                              Instant requestedTime) {
        try {
            // Extract features
            double distance = pickup.distanceTo(destination);
            int hour = requestedTime.atZone(ZoneId.of("Europe/Istanbul")).getHour();
            int dayOfWeek = requestedTime.atZone(ZoneId.of("Europe/Istanbul")).getDayOfWeek().getValue();
            
            // Prepare features
            double[] features = {
                distance,
                hour,
                dayOfWeek,
                pickup.getLat(),
                pickup.getLon()
            };
            
            // Get ML prediction
            double mlPredictedPrice = mlModel.predict(features);
            
            // Get fallback prediction
            PricingResult fallback = fallbackPricingService.calculatePrice(
                pickup, destination, null, false, requestedTime, null
            );
            
            // Blend predictions (ML with 70% weight, traditional with 30%)
            double blendedPrice = (mlPredictedPrice * 0.7) + (fallback.getFinalBasePrice() * 0.3);
            
            log.info("ML Price: {}, Traditional: {}, Blended: {}", 
                mlPredictedPrice, fallback.getFinalBasePrice(), blendedPrice);
            
            return blendedPrice;
        } catch (Exception e) {
            log.error("ML prediction failed, using fallback", e);
            return fallbackPricingService.calculatePrice(
                pickup, destination, null, false, requestedTime, null
            ).getFinalBasePrice();
        }
    }
}
```

---

### 5. Adding Promotional Code Support

**Location**: Modify `TripRequestService.createTripRequest()`

**Implementation**:

1. **Add promotion code entity**
   ```java
   @Entity
   public class PromotionCode {
       @Id
       private String code;
       
       private double discountPercentage;  // e.g., 0.10 for 10% off
       private boolean active;
       private LocalDate expiryDate;
       private int maxUsageCount;
       private int currentUsageCount;
   }
   ```

2. **Update CreateTripRequestRequest DTO**
   ```java
   @Data
   public class CreateTripRequestRequest {
       // ... existing fields ...
       private String promotionCode;  // Optional
   }
   ```

3. **Validate and apply promotion code**
   ```java
   public TripRequestDTO createTripRequest(UUID passengerId, CreateTripRequestRequest request) {
       // ... existing code ...
       
       // Calculate base price
       PricingResult pricingResult = pricingService.calculatePrice(...);
       double discountedMinPrice = pricingResult.getMinPrice();
       double discountedMaxPrice = pricingResult.getMaxPrice();
       
       // Apply promotion code if provided
       if (request.getPromotionCode() != null) {
           PromotionCode promo = promotionCodeRepository
               .findById(request.getPromotionCode())
               .orElseThrow(() -> new IllegalArgumentException("Invalid promotion code"));
           
           if (!promo.isActive() || promo.getExpiryDate().isBefore(LocalDate.now())) {
               throw new IllegalArgumentException("Promotion code expired");
           }
           
           if (promo.getCurrentUsageCount() >= promo.getMaxUsageCount()) {
               throw new IllegalArgumentException("Promotion code usage limit reached");
           }
           
           double discount = 1.0 - promo.getDiscountPercentage();
           discountedMinPrice = pricingResult.getMinPrice() * discount;
           discountedMaxPrice = pricingResult.getMaxPrice() * discount;
           
           // Increment usage count
           promo.setCurrentUsageCount(promo.getCurrentUsageCount() + 1);
           promotionCodeRepository.save(promo);
       }
       
       // Set prices
       tripRequest.setMinPrice(Money.ofTRY(discountedMinPrice));
       tripRequest.setMaxPrice(Money.ofTRY(discountedMaxPrice));
       
       // ... save and return ...
   }
   ```

---

### 6. Implementing Geolocation-Based Pricing

**Location**: Create new `service/pricing/GeolocationPricingService.java`

**Why**: Different areas may have different base fares (e.g., airport surcharges, downtown higher rates)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GeolocationPricingService {
    
    private final PricingConfig pricingConfig;
    private static final double AIRPORT_RADIUS_KM = 5.0;
    private static final double DOWNTOWN_RADIUS_KM = 2.0;
    
    // These would be configurable
    private static final LatLng ISTANBUL_AIRPORT = new LatLng(41.2753, 28.7520);
    private static final LatLng DOWNTOWN_ISTANBUL = new LatLng(41.0082, 28.9784);
    
    public double getLocationMultiplier(Location location) {
        if (isNearAirport(location)) {
            return 1.5;  // 50% surge for airport pickups
        }
        
        if (isDowntown(location)) {
            return 1.1;  // 10% increase for downtown
        }
        
        return 1.0;  // Default
    }
    
    private boolean isNearAirport(Location location) {
        Location airport = new Location(ISTANBUL_AIRPORT.lat, ISTANBUL_AIRPORT.lng);
        return location.distanceTo(airport) < AIRPORT_RADIUS_KM;
    }
    
    private boolean isDowntown(Location location) {
        Location downtown = new Location(DOWNTOWN_ISTANBUL.lat, DOWNTOWN_ISTANBUL.lng);
        return location.distanceTo(downtown) < DOWNTOWN_RADIUS_KM;
    }
}
```

---

## Testing Extensions

### Unit Testing Template

```java
@SpringBootTest
public class PricingServiceExtensionTest {
    
    @Autowired
    private PricingService pricingService;
    
    @MockBean
    private RoutingService routingService;
    
    @Test
    public void testSurgePricingWithHighDemand() {
        // Arrange
        Location pickup = new Location(41.0, 28.9);
        Location destination = new Location(41.1, 29.2);
        when(routingService.calculateRoute(pickup, destination))
            .thenReturn(new RoutingService.RoutingResult(25.0, 38));
        
        // Act
        PricingResult result = pricingService.calculatePrice(
            pickup, destination, null, false, Instant.now(), null);
        
        // Assert
        assertTrue(result.getSurgeMultiplier() > 1.0);
        assertTrue(result.getMaxPrice() > result.getMinPrice());
    }
}
```

---

## Configuration Management

### Dynamic Configuration Reloading

To allow pricing parameter changes without restarting:

```java
@RestController
@RequestMapping("/api/admin/pricing")
@RequiredArgsConstructor
public class PricingConfigController {
    
    private final PricingConfig pricingConfig;
    
    @GetMapping("/config")
    public ResponseEntity<PricingConfig> getConfig() {
        return ResponseEntity.ok(pricingConfig);
    }
    
    @PostMapping("/config")
    public ResponseEntity<PricingConfig> updateConfig(@RequestBody PricingConfig newConfig) {
        // Update fields
        pricingConfig.setBaseFare(newConfig.getBaseFare());
        pricingConfig.setPerKmRate(newConfig.getPerKmRate());
        // ... etc ...
        
        return ResponseEntity.ok(pricingConfig);
    }
}
```

---

## Monitoring and Logging

### Adding Custom Metrics

```java
@Component
public class PricingMetrics {
    
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activePricingRequests = new AtomicInteger(0);
    
    public PricingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.gauge("pricing.active_requests", activePricingRequests);
    }
    
    public void recordPricingCalculation(long durationMs, double price) {
        meterRegistry.timer("pricing.calculation_duration_ms")
            .record(durationMs, TimeUnit.MILLISECONDS);
        meterRegistry.timer("pricing.final_price")
            .record((long) price);
    }
}
```

---

## Performance Optimization

### Caching Strategies

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CachedPricingService {
    
    private final PricingService pricingService;
    private final Cache<String, PricingResult> priceCache = 
        CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    
    public PricingResult getPricingCached(Location pickup, Location destination) {
        String cacheKey = pickup.getLat() + "," + pickup.getLon() + ":" +
                         destination.getLat() + "," + destination.getLon();
        
        return priceCache.get(cacheKey, () -> 
            pricingService.calculatePrice(pickup, destination, null, false, null, null)
        );
    }
}
```

---

## Deployment Considerations

### Environment-Specific Configuration

```yaml
# application-dev.yaml
pricing:
  baseFare: 20.0
  useGoogleMaps: false  # Use Haversine locally

# application-prod.yaml
pricing:
  baseFare: 25.0
  useGoogleMaps: true   # Use real API in production
  googleMapsApiKey: ${GOOGLE_MAPS_API_KEY}
```

### Health Checks

```java
@Component
public class PricingHealthIndicator extends AbstractHealthIndicator {
    
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            // Test pricing service is working
            Location test1 = new Location(41.0, 28.9);
            Location test2 = new Location(41.1, 29.2);
            pricingService.calculatePrice(test1, test2, null, false, null, null);
            builder.up();
        } catch (Exception e) {
            builder.down().withDetail("error", e.getMessage());
        }
    }
}
```

---

## Troubleshooting Common Issues

### Issue: Prices suddenly too high/low
**Solution**: Check surge calculation, demand/supply values, time multipliers

### Issue: Google Maps API rate limiting
**Solution**: Implement rate limiting, caching, or fallback to Haversine

### Issue: Database geospatial queries slow
**Solution**: Add proper indexes, cache demand/supply data, increase cache TTL

---

## Conclusion

The DriveMe pricing system is designed for **extensibility**. Each component can be enhanced independently:

- **Surge pricing** → Implement real demand/supply queries
- **Weather impact** → Integrate OpenWeather API
- **Loyalty discounts** → Add passenger tier system
- **ML pricing** → Wrap existing service
- **Promotions** → Add promotion code validation
- **Geolocation** → Create location-based multipliers

All extensions follow the same pattern: **update a service method, test thoroughly, deploy with new config**.

For questions, refer to the main implementation documentation or the API reference guide.

