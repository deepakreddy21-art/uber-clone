package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/go-redis/redis/v8"
	"github.com/gorilla/websocket"
	"github.com/segmentio/kafka-go"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

// Global variables
var (
	db          *gorm.DB
	redisClient *redis.Client
	kafkaWriter *kafka.Writer
	kafkaReader *kafka.Reader
	upgrader    = websocket.Upgrader{
		CheckOrigin: func(r *http.Request) bool {
			return true
		},
	}
)

// Models
type DriverLocation struct {
	ID          uint      `json:"id" gorm:"primaryKey"`
	DriverID    uint      `json:"driver_id"`
	Latitude    float64   `json:"latitude"`
	Longitude   float64   `json:"longitude"`
	Geohash     string    `json:"geohash"`
	IsOnline    bool      `json:"is_online"`
	IsAvailable bool      `json:"is_available"`
	VehicleType string    `json:"vehicle_type"`
	Heading     float64   `json:"heading"`
	Speed       float64   `json:"speed"`
	Timestamp   time.Time `json:"timestamp"`
}

type RideRequest struct {
	ID               uint      `json:"id" gorm:"primaryKey"`
	UserID           uint      `json:"user_id"`
	PickupLatitude   float64   `json:"pickup_latitude"`
	PickupLongitude  float64   `json:"pickup_longitude"`
	DropoffLatitude  float64   `json:"dropoff_latitude"`
	DropoffLongitude float64   `json:"dropoff_longitude"`
	PickupGeohash    string    `json:"pickup_geohash"`
	VehicleType      string    `json:"vehicle_type"`
	Status           string    `json:"status"`
	CreatedAt        time.Time `json:"created_at"`
}

type DriverMatch struct {
	DriverID   uint    `json:"driver_id"`
	Distance   float64 `json:"distance"`
	Rating     float64 `json:"rating"`
	Score      float64 `json:"score"`
	ETA        int     `json:"eta"`
	VehicleType string `json:"vehicle_type"`
}

type LocationUpdate struct {
	DriverID    uint    `json:"driver_id"`
	Latitude    float64 `json:"latitude"`
	Longitude   float64 `json:"longitude"`
	Geohash     string  `json:"geohash"`
	IsOnline    bool    `json:"is_online"`
	IsAvailable bool    `json:"is_available"`
	Timestamp   string  `json:"timestamp"`
}

type ETARequest struct {
	DriverLat   float64 `json:"driver_lat"`
	DriverLon   float64 `json:"driver_lon"`
	PickupLat   float64 `json:"pickup_lat"`
	PickupLon   float64 `json:"pickup_lon"`
	VehicleType string  `json:"vehicle_type"`
}

type ETAResponse struct {
	ETA        int     `json:"eta"`
	Distance   float64 `json:"distance"`
	Confidence float64 `json:"confidence"`
}

func main() {
	// Initialize database
	initDatabase()
	
	// Initialize Redis
	initRedis()
	
	// Initialize Kafka
	initKafka()
	
	// Start background workers
	go startLocationProcessor()
	go startDriverMatcher()
	go startETAPredictor()
	
	// Setup HTTP server
	router := gin.Default()
	
	// API routes
	api := router.Group("/api/v1")
	{
		api.POST("/driver/location", updateDriverLocation)
		api.GET("/driver/location/:id", getDriverLocation)
		api.POST("/ride/request", createRideRequest)
		api.GET("/ride/request/:id", getRideRequest)
		api.POST("/eta/predict", predictETA)
		api.GET("/drivers/nearby", getNearbyDrivers)
		api.POST("/driver/match", findDriverMatch)
	}
	
	// WebSocket endpoint
	router.GET("/ws", handleWebSocket)
	
	// Health check
	router.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "healthy", "service": "uber-go-service"})
	})
	
	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}
	
	log.Printf("Starting Go microservice on port %s", port)
	log.Fatal(router.Run(":" + port))
}

func initDatabase() {
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		dsn = "host=localhost user=postgres password=password dbname=uber_clone port=5432 sslmode=disable"
	}
	
	var err error
	db, err = gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}
	
	// Auto migrate
	db.AutoMigrate(&DriverLocation{}, &RideRequest{})
	log.Println("Database connected and migrated")
}

func initRedis() {
	redisAddr := os.Getenv("REDIS_ADDR")
	if redisAddr == "" {
		redisAddr = "localhost:6379"
	}
	
	redisClient = redis.NewClient(&redis.Options{
		Addr:     redisAddr,
		Password: "",
		DB:       0,
	})
	
	ctx := context.Background()
	_, err := redisClient.Ping(ctx).Result()
	if err != nil {
		log.Fatal("Failed to connect to Redis:", err)
	}
	log.Println("Redis connected")
}

func initKafka() {
	kafkaAddr := os.Getenv("KAFKA_ADDR")
	if kafkaAddr == "" {
		kafkaAddr = "localhost:9092"
	}
	
	kafkaWriter = &kafka.Writer{
		Addr:     kafka.TCP(kafkaAddr),
		Topic:    "driver-location-updates",
		Balancer: &kafka.LeastBytes{},
	}
	
	kafkaReader = kafka.NewReader(kafka.ReaderConfig{
		Brokers:  []string{kafkaAddr},
		Topic:    "ride-requests",
		GroupID:  "go-service-group",
		MinBytes: 10e3, // 10KB
		MaxBytes: 10e6, // 10MB
	})
	
	log.Println("Kafka connected")
}

func updateDriverLocation(c *gin.Context) {
	var update LocationUpdate
	if err := c.ShouldBindJSON(&update); err != nil {
		c.JSON(400, gin.H{"error": err.Error()})
		return
	}
	
	// Update database
	driverLoc := DriverLocation{
		DriverID:    update.DriverID,
		Latitude:    update.Latitude,
		Longitude:   update.Longitude,
		Geohash:     update.Geohash,
		IsOnline:    update.IsOnline,
		IsAvailable: update.IsAvailable,
		Timestamp:   time.Now(),
	}
	
	result := db.Save(&driverLoc)
	if result.Error != nil {
		c.JSON(500, gin.H{"error": "Failed to update driver location"})
		return
	}
	
	// Cache in Redis for fast access
	ctx := context.Background()
	key := fmt.Sprintf("driver:location:%d", update.DriverID)
	driverLocJSON, _ := json.Marshal(driverLoc)
	redisClient.Set(ctx, key, driverLocJSON, 5*time.Minute)
	
	// Publish to Kafka for real-time processing
	kafkaMsg, _ := json.Marshal(update)
	kafkaWriter.WriteMessages(context.Background(), kafka.Message{
		Key:   []byte(fmt.Sprintf("%d", update.DriverID)),
		Value: kafkaMsg,
	})
	
	c.JSON(200, gin.H{"message": "Driver location updated", "driver_id": update.DriverID})
}

func getDriverLocation(c *gin.Context) {
	driverID := c.Param("id")
	
	// Try Redis first
	ctx := context.Background()
	key := fmt.Sprintf("driver:location:%s", driverID)
	cached, err := redisClient.Get(ctx, key).Result()
	
	if err == nil {
		var driverLoc DriverLocation
		json.Unmarshal([]byte(cached), &driverLoc)
		c.JSON(200, driverLoc)
		return
	}
	
	// Fallback to database
	var driverLoc DriverLocation
	result := db.Where("driver_id = ?", driverID).First(&driverLoc)
	if result.Error != nil {
		c.JSON(404, gin.H{"error": "Driver location not found"})
		return
	}
	
	c.JSON(200, driverLoc)
}

func createRideRequest(c *gin.Context) {
	var request RideRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(400, gin.H{"error": err.Error()})
		return
	}
	
	request.CreatedAt = time.Now()
	request.Status = "PENDING"
	
	result := db.Create(&request)
	if result.Error != nil {
		c.JSON(500, gin.H{"error": "Failed to create ride request"})
		return
	}
	
	// Publish to Kafka for driver matching
	kafkaMsg, _ := json.Marshal(request)
	kafkaWriter.WriteMessages(context.Background(), kafka.Message{
		Key:   []byte("ride-request"),
		Value: kafkaMsg,
	})
	
	c.JSON(201, gin.H{"message": "Ride request created", "id": request.ID})
}

func getRideRequest(c *gin.Context) {
	requestID := c.Param("id")
	
	var request RideRequest
	result := db.First(&request, requestID)
	if result.Error != nil {
		c.JSON(404, gin.H{"error": "Ride request not found"})
		return
	}
	
	c.JSON(200, request)
}

func predictETA(c *gin.Context) {
	var etaReq ETARequest
	if err := c.ShouldBindJSON(&etaReq); err != nil {
		c.JSON(400, gin.H{"error": err.Error()})
		return
	}
	
	// Calculate distance using Haversine formula
	distance := haversineDistance(etaReq.DriverLat, etaReq.DriverLon, etaReq.PickupLat, etaReq.PickupLon)
	
	// Predict ETA using ML model (simplified for demo)
	eta := predictETAML(etaReq, distance)
	
	response := ETAResponse{
		ETA:        eta,
		Distance:   distance,
		Confidence: 0.85, // ML model confidence
	}
	
	c.JSON(200, response)
}

func getNearbyDrivers(c *gin.Context) {
	lat, _ := strconv.ParseFloat(c.Query("lat"), 64)
	lon, _ := strconv.ParseFloat(c.Query("lon"), 64)
	radius, _ := strconv.ParseFloat(c.Query("radius"), 64)
	vehicleType := c.Query("vehicle_type")
	
	if radius == 0 {
		radius = 5.0 // Default 5km radius
	}
	
	// Get geohash prefix for efficient querying
	geohashPrefix := generateGeohashPrefix(lat, lon, radius)
	
	var drivers []DriverLocation
	query := db.Where("geohash LIKE ? AND is_online = ? AND is_available = ?", geohashPrefix+"%", true, true)
	
	if vehicleType != "" {
		query = query.Where("vehicle_type = ?", vehicleType)
	}
	
	query.Find(&drivers)
	
	// Calculate distances and filter by radius
	var nearbyDrivers []DriverLocation
	for _, driver := range drivers {
		distance := haversineDistance(lat, lon, driver.Latitude, driver.Longitude)
		if distance <= radius {
			driver.Speed = distance // Use distance as speed for demo
			nearbyDrivers = append(nearbyDrivers, driver)
		}
	}
	
	c.JSON(200, gin.H{
		"drivers": nearbyDrivers,
		"count":   len(nearbyDrivers),
		"radius":  radius,
	})
}

func findDriverMatch(c *gin.Context) {
	var request RideRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(400, gin.H{"error": err.Error()})
		return
	}
	
	// Find nearby available drivers
	geohashPrefix := generateGeohashPrefix(request.PickupLatitude, request.PickupLongitude, 5.0)
	
	var drivers []DriverLocation
	query := db.Where("geohash LIKE ? AND is_online = ? AND is_available = ?", geohashPrefix+"%", true, true)
	
	if request.VehicleType != "" {
		query = query.Where("vehicle_type = ?", request.VehicleType)
	}
	
	query.Find(&drivers)
	
	// Score and rank drivers
	var matches []DriverMatch
	for _, driver := range drivers {
		distance := haversineDistance(request.PickupLatitude, request.PickupLongitude, driver.Latitude, driver.Longitude)
		eta := int(distance * 2) // Simple ETA calculation
		
		// Calculate score (distance + rating + availability)
		score := calculateDriverScore(distance, 4.5, driver.IsAvailable) // Assuming 4.5 rating
		
		match := DriverMatch{
			DriverID:    driver.DriverID,
			Distance:    distance,
			Rating:      4.5,
			Score:       score,
			ETA:         eta,
			VehicleType: driver.VehicleType,
		}
		
		matches = append(matches, match)
	}
	
	// Sort by score (highest first)
	sortDriversByScore(matches)
	
	c.JSON(200, gin.H{
		"matches": matches,
		"count":   len(matches),
	})
}

func handleWebSocket(c *gin.Context) {
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Println("WebSocket upgrade failed:", err)
		return
	}
	defer conn.Close()
	
	// Handle WebSocket messages
	for {
		messageType, message, err := conn.ReadMessage()
		if err != nil {
			log.Println("WebSocket read error:", err)
			break
		}
		
		// Echo message back (for demo)
		err = conn.WriteMessage(messageType, message)
		if err != nil {
			log.Println("WebSocket write error:", err)
			break
		}
	}
}

// Background workers
func startLocationProcessor() {
	log.Println("Starting location processor...")
	
	for {
		ctx := context.Background()
		message, err := kafkaReader.ReadMessage(ctx)
		if err != nil {
			log.Println("Error reading Kafka message:", err)
			continue
		}
		
		var locationUpdate LocationUpdate
		if err := json.Unmarshal(message.Value, &locationUpdate); err != nil {
			log.Println("Error unmarshaling location update:", err)
			continue
		}
		
		// Process location update
		processLocationUpdate(locationUpdate)
	}
}

func startDriverMatcher() {
	log.Println("Starting driver matcher...")
	
	// Process ride requests and find best drivers
	for {
		time.Sleep(10 * time.Second)
		// Process pending ride requests
		processPendingRideRequests()
	}
}

func startETAPredictor() {
	log.Println("Starting ETA predictor...")
	
	// Continuously improve ETA predictions using ML
	for {
		time.Sleep(30 * time.Second)
		// Update ML model with new data
		updateETAModel()
	}
}

// Utility functions
func haversineDistance(lat1, lon1, lat2, lon2 float64) float64 {
	const R = 6371 // Earth's radius in kilometers
	
	lat1Rad := lat1 * (3.14159 / 180)
	lat2Rad := lat2 * (3.14159 / 180)
	deltaLat := (lat2 - lat1) * (3.14159 / 180)
	deltaLon := (lon2 - lon1) * (3.14159 / 180)
	
	a := (deltaLat/2)*(deltaLat/2) + lat1Rad*lat2Rad*(deltaLon/2)*(deltaLon/2)
	c := 2 * atan2(sqrt(a), sqrt(1-a))
	
	return R * c
}

func atan2(y, x float64) float64 {
	if x > 0 {
		return atan(y / x)
	} else if x < 0 && y >= 0 {
		return atan(y/x) + 3.14159
	} else if x < 0 && y < 0 {
		return atan(y/x) - 3.14159
	} else if x == 0 && y > 0 {
		return 3.14159 / 2
	} else if x == 0 && y < 0 {
		return -3.14159 / 2
	}
	return 0
}

func atan(x float64) float64 {
	// Simplified atan implementation
	return x - (x*x*x)/3 + (x*x*x*x*x)/5
}

func sqrt(x float64) float64 {
	// Simplified sqrt implementation
	z := x / 2
	for i := 0; i < 10; i++ {
		z = z - (z*z-x)/(2*z)
	}
	return z
}

func generateGeohashPrefix(lat, lon, radius float64) string {
	// Simplified geohash prefix generation
	// In production, use proper geohash library
	return "abc123"
}

func calculateDriverScore(distance, rating float64, available bool) float64 {
	// Distance penalty (closer is better)
	distanceScore := 10 - distance
	if distanceScore < 0 {
		distanceScore = 0
	}
	
	// Rating score (0-5 scale)
	ratingScore := rating * 2
	
	// Availability bonus
	availabilityBonus := 0.0
	if available {
		availabilityBonus = 1.0
	}
	
	// Weighted score
	return (distanceScore * 0.6) + (ratingScore * 0.3) + (availabilityBonus * 0.1)
}

func sortDriversByScore(matches []DriverMatch) {
	// Simple bubble sort for demo
	for i := 0; i < len(matches)-1; i++ {
		for j := 0; j < len(matches)-i-1; j++ {
			if matches[j].Score < matches[j+1].Score {
				matches[j], matches[j+1] = matches[j+1], matches[j]
			}
		}
	}
}

func predictETAML(req ETARequest, distance float64) int {
	// Simplified ML prediction
	// In production, use TensorFlow, PyTorch, or custom ML model
	
	// Base ETA calculation
	baseETA := int(distance * 2) // 2 minutes per km
	
	// Time-based adjustments
	hour := time.Now().Hour()
	if hour >= 7 && hour <= 9 || hour >= 17 && hour <= 19 {
		baseETA = int(float64(baseETA) * 1.3) // Peak hours
	}
	
	// Vehicle type adjustments
	switch req.VehicleType {
	case "PREMIUM":
		baseETA = int(float64(baseETA) * 0.9) // Premium vehicles are faster
	case "POOL":
		baseETA = int(float64(baseETA) * 1.2) // Pool rides take longer
	}
	
	return baseETA
}

func processLocationUpdate(update LocationUpdate) {
	// Process real-time location updates
	log.Printf("Processing location update for driver %d: lat=%.6f, lon=%.6f", 
		update.DriverID, update.Latitude, update.Longitude)
	
	// Update real-time analytics
	// Send notifications if needed
	// Update traffic patterns
}

func processPendingRideRequests() {
	// Find and process pending ride requests
	var requests []RideRequest
	db.Where("status = ?", "PENDING").Find(&requests)
	
	for _, request := range requests {
		// Find best driver match
		// Update request status
		// Send notifications
		log.Printf("Processing ride request %d", request.ID)
	}
}

func updateETAModel() {
	// Update ML model with new data
	log.Println("Updating ETA prediction model...")
	
	// In production:
	// - Collect new trip data
	// - Retrain model
	// - Update model weights
	// - A/B test new predictions
}
