package com.uberclone.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uberclone.backend.model.Ride;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MLIntegrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Value("${ml.service.enabled:true}")
    private boolean mlServiceEnabled;

    /**
     * Get AI-powered ETA prediction
     */
    public Optional<Integer> predictETAWithML(double pickupLat, double pickupLon, 
                                           double dropoffLat, double dropoffLon,
                                           String vehicleType) {
        if (!mlServiceEnabled) {
            log.debug("ML service is disabled, using fallback ETA calculation");
            return Optional.empty();
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("pickup_lat", pickupLat);
            request.put("pickup_lon", pickupLon);
            request.put("dropoff_lat", dropoffLat);
            request.put("dropoff_lon", dropoffLon);
            request.put("vehicle_type", vehicleType);
            request.put("hour_of_day", LocalTime.now().getHour());
            request.put("day_of_week", LocalDateTime.now().getDayOfWeek().getValue());
            request.put("weather_condition", "CLEAR"); // Would come from weather service
            request.put("traffic_level", "NORMAL"); // Would come from traffic service

            String url = mlServiceUrl + "/predict/eta";
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Integer eta = (Integer) body.get("eta_minutes");
                Double confidence = (Double) body.get("confidence");
                
                log.info("ML ETA prediction: {} minutes with confidence: {}", eta, confidence);
                return Optional.of(eta);
            }

        } catch (ResourceAccessException e) {
            log.warn("ML service unavailable: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error getting ML ETA prediction: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Get AI-powered demand forecasting
     */
    public Optional<Integer> forecastDemandWithML(double latitude, double longitude, 
                                               String timeRange, LocalDateTime date) {
        if (!mlServiceEnabled) {
            return Optional.empty();
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("location_lat", latitude);
            request.put("location_lon", longitude);
            request.put("time_range", timeRange);
            request.put("date", date.toLocalDate().toString());

            String url = mlServiceUrl + "/forecast/demand";
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Integer predictedDemand = (Integer) body.get("predicted_demand");
                Double confidence = (Double) body.get("confidence");
                
                log.info("ML demand forecast: {} rides with confidence: {}", predictedDemand, confidence);
                return Optional.of(predictedDemand);
            }

        } catch (Exception e) {
            log.error("Error getting ML demand forecast: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Get AI-powered surge pricing
     */
    public Optional<BigDecimal> predictSurgePricingWithML(double latitude, double longitude,
                                                         int currentDemand, int availableDrivers,
                                                         LocalDateTime time) {
        if (!mlServiceEnabled) {
            return Optional.empty();
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("location_lat", latitude);
            request.put("location_lon", longitude);
            request.put("current_demand", currentDemand);
            request.put("available_drivers", availableDrivers);
            request.put("time_of_day", time.toLocalTime().toString());
            request.put("day_type", getDayType(time));

            String url = mlServiceUrl + "/predict/surge";
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Double surgeMultiplier = (Double) body.get("surge_multiplier");
                Double confidence = (Double) body.get("confidence");
                
                log.info("ML surge pricing: {}x with confidence: {}", surgeMultiplier, confidence);
                return Optional.of(BigDecimal.valueOf(surgeMultiplier));
            }

        } catch (Exception e) {
            log.error("Error getting ML surge pricing: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Get AI-powered driver matching recommendations
     */
    public Optional<List<Map<String, Object>>> getDriverMatchingRecommendations(
            double pickupLat, double pickupLon, String vehicleType, int passengerCount) {
        if (!mlServiceEnabled) {
            return Optional.empty();
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("pickup_lat", pickupLat);
            request.put("pickup_lon", pickupLon);
            request.put("vehicle_type", vehicleType);
            request.put("passenger_count", passengerCount);
            request.put("special_requirements", List.of());

            String url = mlServiceUrl + "/match/drivers";
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(url, request, new HashMap<String, Object>().getClass());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> matches = (List<Map<String, Object>>) body.get("matches");
                
                log.info("ML driver matching: {} recommendations", matches.size());
                return Optional.of(matches);
            }

        } catch (Exception e) {
            log.error("Error getting ML driver matching: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Train ML models with new data
     */
    public void triggerModelTraining(String modelType) {
        if (!mlServiceEnabled) {
            return;
        }

        try {
            String url = mlServiceUrl + "/train/" + modelType + "-model";
            restTemplate.postForEntity(url, null, String.class);
            log.info("Triggered training for {} model", modelType);
        } catch (Exception e) {
            log.error("Error triggering model training: {}", e.getMessage(), e);
        }
    }

    /**
     * Get ML service health status
     */
    public boolean isMLServiceHealthy() {
        if (!mlServiceEnabled) {
            return false;
        }

        try {
            String url = mlServiceUrl + "/health";
            ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(url, new HashMap<String, Object>().getClass());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("ML service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get ML service capabilities
     */
    public Map<String, Object> getMLServiceCapabilities() {
        if (!mlServiceEnabled) {
            return Map.of("enabled", false);
        }

        try {
            String url = mlServiceUrl + "/health";
            ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(url, new HashMap<String, Object>().getClass());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> capabilities = new HashMap<>();
                capabilities.put("enabled", true);
                capabilities.put("ml_frameworks", response.getBody().get("ml_frameworks"));
                capabilities.put("models", response.getBody().get("models"));
                return capabilities;
            }
        } catch (Exception e) {
            log.error("Error getting ML service capabilities: {}", e.getMessage(), e);
        }

        return Map.of("enabled", false, "error", "Service unavailable");
    }

    /**
     * Determine day type for ML service
     */
    private String getDayType(LocalDateTime time) {
        int dayOfWeek = time.getDayOfWeek().getValue();
        
        // Simple logic - could be enhanced with holiday detection
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return "weekend";
        } else {
            return "weekday";
        }
    }

    /**
     * Batch process ride data for ML training
     */
    public void sendRideDataForTraining(List<Ride> rides) {
        if (!mlServiceEnabled || rides.isEmpty()) {
            return;
        }

        try {
            // Convert rides to training data format
            List<Map<String, Object>> trainingData = rides.stream()
                .map(this::convertRideToTrainingData)
                .toList();

            // Send to ML service for training
            String url = mlServiceUrl + "/training/ride-data";
            restTemplate.postForEntity(url, trainingData, String.class);
            
            log.info("Sent {} ride records for ML training", rides.size());
        } catch (Exception e) {
            log.error("Error sending ride data for training: {}", e.getMessage(), e);
        }
    }

    /**
     * Convert Ride entity to ML training data format
     */
    private Map<String, Object> convertRideToTrainingData(Ride ride) {
        Map<String, Object> trainingData = new HashMap<>();
        
        trainingData.put("pickup_lat", ride.getPickupLatitude());
        trainingData.put("pickup_lon", ride.getPickupLongitude());
        trainingData.put("dropoff_lat", ride.getDropoffLatitude());
        trainingData.put("dropoff_lon", ride.getDropoffLongitude());
        trainingData.put("distance", ride.getDistance());
        trainingData.put("duration", ride.getActualDuration());
        trainingData.put("fare", ride.getTotalFare());
        // Note: Ride model doesn't have vehicleType field
        // trainingData.put("vehicle_type", ride.getVehicleType());
        trainingData.put("hour_of_day", ride.getRequestedAt().getHour());
        trainingData.put("day_of_week", ride.getRequestedAt().getDayOfWeek().getValue());
        trainingData.put("month", ride.getRequestedAt().getMonthValue());
        trainingData.put("is_weekend", ride.getRequestedAt().getDayOfWeek().getValue() > 5);
        
        return trainingData;
    }
}
