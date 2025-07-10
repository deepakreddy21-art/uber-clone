# Uber Clone Backend

This is the Java Spring Boot backend for the Uber Clone project.

## Requirements
- Java 17 or higher
- Maven 3.8+

## Getting Started

1. Install Java and Maven.
2. Navigate to this directory:
   ```sh
   cd backend
   ```
3. Build the project:
   ```sh
   mvn clean install
   ```
4. Run the application:
   ```sh
   mvn spring-boot:run
   ```

The backend will start on [http://localhost:8080](http://localhost:8080)

---

## Project Structure
- `src/main/java` - Java source code
- `src/main/resources` - Configuration files
- `pom.xml` - Maven build file 

---

## Database Setup (PostgreSQL)

1. Install PostgreSQL and create a database named `uber_clone`.
2. Update `src/main/resources/application.properties` with your PostgreSQL username and password.
3. The backend will auto-create tables on startup. 

---

## API Documentation

### POST /api/rides/request
Request a new ride. Requires authentication.
- Body: `{ "pickupLocation": string, "dropoffLocation": string }`
- Response: Ride object

### GET /api/rides/available
Get all available (requested) rides.
- Response: List of Ride objects

### POST /api/rides/accept/{rideId}
Accept a ride as a driver. Requires authentication.
- Path: `rideId` (number)
- Response: Ride object or error

### GET /api/rides/my
Get all rides for the authenticated user (driver or rider).
- Response: List of Ride objects

### POST /api/rides/predict-eta
Predict ETA for a ride.
- Body: `{ "pickupLat": number, "pickupLng": number, "dropoffLat": number, "dropoffLng": number, "hourOfDay": number }`
- Response: `{ "etaMinutes": number }`

--- 