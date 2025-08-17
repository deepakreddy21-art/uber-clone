# Uber Clone Backend

A comprehensive Spring Boot backend for an Uber-like ride-sharing application with advanced features including geohashing, real-time driver matching, and comprehensive ride management.

## 🚀 Features

### Core Functionality
- **User Management**: User registration, authentication, and role-based access control
- **Driver Management**: Driver registration, approval, and profile management
- **Ride Management**: Complete ride lifecycle from request to completion
- **Real-time Updates**: WebSocket-based real-time communication
- **Payment Integration**: Stripe payment processing
- **Rating System**: Bidirectional rating system for users and drivers

### Advanced Features
- **Geohashing**: Efficient spatial indexing for driver-passenger matching
- **Smart Driver Matching**: AI-powered driver assignment based on proximity, rating, and availability
- **Dynamic Pricing**: Surge pricing, peak hour multipliers, and vehicle type pricing
- **ETA Prediction**: Real-time ETA calculations using distance and traffic patterns
- **Location Tracking**: Real-time GPS tracking with geohash optimization
- **Notification System**: Push notifications, SMS, and email notifications

## 🏗️ Architecture

### Technology Stack
- **Framework**: Spring Boot 3.2.6
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: Spring Security with JWT authentication
- **Real-time**: WebSocket with STOMP
- **Payment**: Stripe integration
- **Build Tool**: Maven
- **Java Version**: 17

### Key Components

#### Models
- **User**: Comprehensive user model with driver-specific fields
- **Ride**: Complete ride information with status tracking
- **RideRequest**: Ride request management with geohashing
- **DriverLocation**: Real-time driver location tracking
- **PaymentTransaction**: Payment processing and tracking

#### Services
- **GeohashService**: Spatial indexing and location calculations
- **DriverMatchingService**: Intelligent driver assignment algorithm
- **PricingService**: Dynamic fare calculation with surge pricing
- **RideService**: Complete ride lifecycle management
- **NotificationService**: Multi-channel notification system

#### Controllers
- **RideController**: Ride management endpoints
- **DriverController**: Driver-specific operations
- **UserController**: User management and authentication
- **PaymentController**: Payment processing endpoints

#### WebSocket
- **RideStatusWebSocketController**: Real-time ride updates and notifications

## 🔧 Setup and Installation

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Redis (optional, for caching)

### Database Setup
1. Create a PostgreSQL database
2. Update `application.properties` with your database credentials
3. The application will automatically create tables on startup

### Configuration
Update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/uber_clone
spring.datasource.username=your_username
spring.datasource.password=your_password

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT Configuration
jwt.secret=your_jwt_secret_key
jwt.expiration=86400000

# Stripe Configuration
stripe.secret.key=your_stripe_secret_key
stripe.publishable.key=your_stripe_publishable_key

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

### Running the Application
```bash
# Clone the repository
git clone <repository-url>
cd uber-clone/backend

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

## 📡 API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User authentication
- `POST /api/auth/refresh` - Refresh JWT token

### Rides
- `POST /api/rides/request` - Create ride request
- `GET /api/rides/request/{requestId}` - Get ride request status
- `POST /api/rides/request/{requestId}/cancel` - Cancel ride request
- `POST /api/rides/{rideId}/status` - Update ride status
- `POST /api/rides/{rideId}/rate` - Rate a ride
- `POST /api/rides/{rideId}/cancel` - Cancel a ride
- `GET /api/rides/user/{userId}/history` - Get user ride history
- `GET /api/rides/driver/{driverId}/history` - Get driver ride history
- `POST /api/rides/fare/estimate` - Estimate ride fare
- `GET /api/rides/drivers/available` - Get available drivers in area
- `POST /api/rides/eta` - Calculate ETA

### Drivers
- `POST /api/drivers/{driverId}/location` - Update driver location
- `GET /api/drivers/{driverId}/location` - Get driver location
- `POST /api/drivers/{driverId}/status` - Set driver status
- `GET /api/drivers/{driverId}/ride-requests` - Get available ride requests
- `POST /api/drivers/{driverId}/accept-ride/{requestId}` - Accept ride request
- `POST /api/drivers/{driverId}/rides/{rideId}/status` - Update ride status
- `GET /api/drivers/{driverId}/current-ride` - Get current ride
- `GET /api/drivers/{driverId}/rides/history` - Get ride history
- `POST /api/drivers/{driverId}/rides/{rideId}/rate-passenger` - Rate passenger
- `GET /api/drivers/{driverId}/earnings` - Get earnings summary
- `GET /api/drivers/{driverId}/stats` - Get driver statistics

### WebSocket Endpoints
- `/app/ride-request` - Send ride request updates
- `/app/ride-status` - Send ride status updates
- `/app/driver-location` - Send driver location updates
- `/app/join-ride-tracking` - Join ride tracking
- `/app/leave-ride-tracking` - Leave ride tracking

## 🗺️ Geohashing Implementation

### Overview
The application uses geohashing for efficient spatial queries and driver-passenger matching. Geohashing converts latitude and longitude coordinates into a string representation that allows for quick proximity searches.

### Key Features
- **12-character precision**: Provides approximately 1.2km accuracy
- **Neighbor calculation**: Automatically finds adjacent geohash cells
- **Radius-based search**: Efficiently searches for drivers within specified radius
- **Spatial indexing**: Database queries use geohash prefixes for fast lookups

### Usage Example
```java
// Encode coordinates to geohash
String geohash = geohashService.encode(latitude, longitude);

// Find drivers in area
List<String> geohashPrefixes = geohashService.getGeohashPrefixes(
    latitude, longitude, radiusKm
);

// Calculate distance between points
double distance = geohashService.calculateDistance(
    lat1, lon1, lat2, lon2
);
```

## 🚗 Driver Matching Algorithm

### Matching Criteria
1. **Proximity**: Drivers within specified radius (default: 5km)
2. **Availability**: Online and available drivers only
3. **Rating**: Higher-rated drivers get priority
4. **Vehicle Type**: Match vehicle type preferences
5. **Distance Score**: Closer drivers get higher scores

### Scoring Formula
```
Score = (Distance Score × 0.7) + (Rating Score × 0.3)
Distance Score = max(0, 10 - distance_in_km)
Rating Score = rating × 2
```

### Implementation
```java
// Find available drivers
List<DriverLocation> drivers = driverMatchingService
    .findAvailableDrivers(rideRequest, 5.0);

// Assign best driver
Optional<User> assignedDriver = driverMatchingService
    .assignDriverToRide(rideRequest);
```

## 💰 Dynamic Pricing System

### Base Pricing
- **Base Fare**: $2.50
- **Per Kilometer**: $1.50
- **Per Minute**: $0.30

### Multipliers
- **Vehicle Type**:
  - Standard: 1.0x
  - Comfort: 1.3x
  - Premium: 1.8x
  - Pool: 0.7x
- **Peak Hours**: 1.2x (7-9 AM, 5-7 PM)
- **Surge Pricing**: 1.3x - 1.5x (based on demand)

### Surge Pricing Factors
- Weekend nights (10 PM - 2 AM)
- Business districts during business hours
- High demand areas
- Special events

## 🔄 Real-time Updates

### WebSocket Channels
- **User-specific**: `/user/{userId}/ride`, `/user/{userId}/notifications`
- **Ride-specific**: `/topic/ride/{rideId}/status`, `/topic/ride/{rideId}/driver-location`
- **General**: `/topic/ride-request`, `/topic/ride-status`

### Update Types
- Ride request status changes
- Driver assignment notifications
- ETA updates
- Driver location updates
- Ride completion notifications

## 🧪 Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=GeohashServiceTest

# Run with coverage
mvn jacoco:report
```

### Test Coverage
- Unit tests for all services
- Integration tests for controllers
- Repository tests with test database
- WebSocket message handling tests

## 📊 Performance Considerations

### Database Optimization
- Geohash indexing for spatial queries
- Composite indexes on frequently queried fields
- Connection pooling configuration
- Query optimization with proper JPA annotations

### Caching Strategy
- In-memory caching for active ride requests
- Driver location caching
- Fare calculation caching
- User session caching

### Scalability
- Horizontal scaling with load balancers
- Database read replicas
- Redis clustering for session management
- Microservices architecture ready

## 🔒 Security Features

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (USER, DRIVER, ADMIN)
- Password encryption with BCrypt
- Session management

### Data Protection
- Input validation and sanitization
- SQL injection prevention
- XSS protection
- CORS configuration

### API Security
- Rate limiting
- Request size limits
- Secure headers
- HTTPS enforcement

## 🚀 Deployment

### Docker Deployment
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Variables
```bash
export SPRING_PROFILES_ACTIVE=production
export DATABASE_URL=your_production_db_url
export JWT_SECRET=your_production_jwt_secret
export STRIPE_SECRET_KEY=your_production_stripe_key
```

### Monitoring
- Application metrics with Micrometer
- Health checks with Spring Boot Actuator
- Log aggregation with structured logging
- Performance monitoring with APM tools

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation wiki

## 🔮 Future Enhancements

- **Machine Learning**: Predictive ETA and demand forecasting
- **Multi-language Support**: Internationalization
- **Advanced Analytics**: Driver and user behavior insights
- **IoT Integration**: Smart city and traffic data integration
- **Blockchain**: Decentralized payment and identity management
- **AI Chatbot**: Customer support automation
- **Advanced Routing**: Multi-stop rides and carpooling
- **Safety Features**: Emergency button and ride sharing 