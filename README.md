# DriveMe - Ride-Sharing Platform Demo

A full-stack ride-sharing application built for Bilkent CS492 Semester-1 demo, demonstrating real-time communication between passengers and drivers.

## 🏗️ Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.3.2**
- **PostgreSQL 16** (database)
- **WebSocket/STOMP** (real-time messaging)
- **Spring Data JPA** (ORM)
- **Swagger/OpenAPI** (API documentation)

### Mobile
- **Flutter** (Dart)
- **STOMP WebSocket client** (real-time events)

### DevOps
- **Docker Compose** (PostgreSQL containerization)
- **GitHub Actions** (CI/CD)


## 🚀 Quick Start

### Prerequisites
- **Java 17+** (install from [Adoptium](https://adoptium.net/))
- **Flutter 3.19+** (install from [flutter.dev](https://flutter.dev))
- **Docker Desktop** (install from [docker.com](https://www.docker.com/products/docker-desktop/))
- **Android Studio** or **VS Code** with Flutter plugin

### 1️⃣ Start PostgreSQL Database

```powershell
cd infra
docker compose up -d
```

Verify it's running:
```powershell
docker ps
```

You should see `driveme-postgres` running on port 5432.

### 2️⃣ Start Backend

Open a new terminal:

```powershell
cd backend
./gradlew bootRun
```

Or on Windows:
```powershell
cd backend
.\gradlew.bat bootRun
```

The backend will start on **http://localhost:8080**

**Swagger UI** (API documentation): http://localhost:8080/swagger-ui/index.html

### 3️⃣ Start Flutter Mobile App

Open a new terminal:

```powershell
cd mobile
flutter pub get
flutter run
```

- If using **Android emulator**, it will automatically use `ws://10.0.2.2:8080/ws` to connect to your host machine's backend
- If using **physical device**, update the WebSocket URL in [driver_home.dart](mobile/lib/screens/driver_home.dart#L18) to your machine's local IP (e.g., `ws://192.168.1.100:8080/ws`)

## 🎯 Demo Flow (Test End-to-End)

### Test Real-Time WebSocket Communication

1. **Launch the Flutter app** (Driver screen will open)
2. **Create a ride request** using one of these methods:

#### Option A: Using Swagger UI
1. Go to http://localhost:8080/swagger-ui/index.html
2. Find `POST /api/requests`
3. Click "Try it out"
4. Use this sample JSON:
```json
{
  "pickupLat": 39.925,
  "pickupLng": 32.836,
  "destLat": 39.92,
  "destLng": 32.85
}
```
5. Click "Execute"

#### Option B: Using curl
```powershell
curl -X POST http://localhost:8080/api/requests `
  -H "Content-Type: application/json" `
  -d '{\"pickupLat\":39.925, \"pickupLng\":32.836, \"destLat\":39.92, \"destLng\":32.85}'
```

#### Option C: Using PowerShell Invoke-RestMethod
```powershell
$body = @{
    pickupLat = 39.925
    pickupLng = 32.836
    destLat = 39.92
    destLng = 32.85
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/requests" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

3. **✅ Watch the Flutter app** – The ride request should appear **instantly** in the Driver screen!

This demonstrates:
- ✅ REST API working (POST endpoint)
- ✅ Database persistence (PostgreSQL)
- ✅ Real-time WebSocket broadcast (STOMP)
- ✅ Flutter client receiving live updates

## 🔧 Development Tips

### Backend Hot Reload
Use Spring Boot DevTools (already included) for automatic restart on code changes.

### Check Database
Connect to PostgreSQL:
```powershell
docker exec -it driveme-postgres psql -U driveme -d driveme
```

View ride requests:
```sql
SELECT * FROM ride_request;
```

Exit: `\q`

### Stop Services
```powershell
# Stop backend: Ctrl+C in the backend terminal

# Stop Flutter: Ctrl+C in the mobile terminal

# Stop PostgreSQL:
cd infra
docker compose down
```

## 📊 CI/CD

GitHub Actions workflows automatically run on push/PR:
- **Backend**: Build + tests ([.github/workflows/backend-ci.yml](.github/workflows/backend-ci.yml))
- **Mobile**: Flutter analyze + tests ([.github/workflows/mobile-ci.yml](.github/workflows/mobile-ci.yml))

## 🎓 Semester-1 Demo Checklist

- [x] Installed tech stack (Java 17, Spring Boot, Flutter, PostgreSQL, Docker)
- [x] Git repository with branching
- [x] CI/CD pipelines (GitHub Actions)
- [x] Backend REST API with Swagger documentation
- [x] Database integration (PostgreSQL + JPA)
- [x] Mobile app with working screens
- [x] End-to-end integration (create request → broadcast → mobile receives)

## 🔜 Next Steps (Semester 2)

1. **Authentication & Authorization**
   - JWT tokens
   - User registration/login
   - Role-based access (Passenger/Driver)

2. **Core Business Logic**
   - Driver offers
   - Trip matching & acceptance
   - QR code verification
   - Trip tracking with GPS

3. **Google Maps Integration**
   - Map display
   - Route calculation
   - Location services

4. **Data Privacy (KVKK/GDPR)**
   - Minimal location storage
   - Data retention policies
   - User consent management

## 📝 Troubleshooting

### Backend won't start
- Ensure Java 17+ is installed: `java -version`
- Check PostgreSQL is running: `docker ps`
- Verify port 8080 is not in use

### Flutter can't connect to backend
- On Android emulator, use `ws://10.0.2.2:8080/ws`
- On physical device, use your machine's local IP
- Ensure backend is running: http://localhost:8080/swagger-ui/index.html

### Database connection errors
- Verify PostgreSQL container is running
- Check credentials in [application.yml](backend/src/main/resources/application.yml)

## 📄 License

This is a student project for Bilkent University CS491-492.

---

**Project Team**: DriveMe  
**Course**: CS491 - Senior Project  
**Semester**: Fall 2025 
