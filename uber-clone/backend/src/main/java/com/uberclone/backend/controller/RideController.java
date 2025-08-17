package com.uberclone.backend.controller;

import com.uberclone.backend.model.Ride;
import com.uberclone.backend.model.RideRequest;
import com.uberclone.backend.service.DriverMatchingService;
import com.uberclone.backend.service.PricingService;
import com.uberclone.backend.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RideController {

    private final RideService rideService;
    private final DriverMatchingService driverMatchingService;
    private final PricingService pricingService;

    /**
     * Create a new ride request
     */
    @PostMapping("/request")
    public ResponseEntity<RideRequest> createRideRequest(
            @RequestBody RideService.RideRequestRequest request,
            Authentication authentication) {
        
        log.info("Creating ride request: {}", request);
        
        try {
            RideRequest rideRequest = rideService.createRideRequest(request);
            return ResponseEntity.ok(rideRequest);
        } catch (Exception e) {
            log.error("Error creating ride request", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get ride request status
     */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<RideRequest> getRideRequestStatus(@PathVariable Long requestId) {
        // This would typically fetch from a repository
        // For now, return a placeholder response
        return ResponseEntity.ok().build();
    }

    /**
     * Cancel ride request
     */
    @PostMapping("/request/{requestId}/cancel")
    public ResponseEntity<?> cancelRideRequest(@PathVariable Long requestId) {
        log.info("Cancelling ride request: {}", requestId);
        
        try {
            // Implementation would go here
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error cancelling ride request", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update ride status
     */
    @PostMapping("/{rideId}/status")
    public ResponseEntity<Ride> updateRideStatus(
            @PathVariable Long rideId,
            @RequestBody Map<String, Object> request) {
        
        String newStatus = (String) request.get("status");
        String additionalInfo = (String) request.get("additionalInfo");
        
        log.info("Updating ride {} status to: {}", rideId, newStatus);
        
        try {
            Ride.Status status = Ride.Status.valueOf(newStatus.toUpperCase());
            Ride updatedRide = rideService.updateRideStatus(rideId, status, additionalInfo);
            return ResponseEntity.ok(updatedRide);
        } catch (Exception e) {
            log.error("Error updating ride status", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Rate a ride
     */
    @PostMapping("/{rideId}/rate")
    public ResponseEntity<?> rateRide(
            @PathVariable Long rideId,
            @RequestBody Map<String, Object> request) {
        
        Integer rating = (Integer) request.get("rating");
        String review = (String) request.get("review");
        Boolean isUserRating = (Boolean) request.get("isUserRating");
        
        log.info("Rating ride {}: rating={}, isUserRating={}", rideId, rating, isUserRating);
        
        try {
            rideService.rateRide(rideId, rating, review, isUserRating);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error rating ride", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cancel a ride
     */
    @PostMapping("/{rideId}/cancel")
    public ResponseEntity<Ride> cancelRide(
            @PathVariable Long rideId,
            @RequestBody Map<String, Object> request) {
        
        String reason = (String) request.get("reason");
        Boolean cancelledByUser = (Boolean) request.get("cancelledByUser");
        
        log.info("Cancelling ride {}: reason={}, cancelledByUser={}", rideId, reason, cancelledByUser);
        
        try {
            Ride cancelledRide = rideService.cancelRide(rideId, reason, cancelledByUser);
            return ResponseEntity.ok(cancelledRide);
        } catch (Exception e) {
            log.error("Error cancelling ride", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user's ride history
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<Ride>> getUserRideHistory(@PathVariable Long userId) {
        log.info("Getting ride history for user: {}", userId);
        
        try {
            List<Ride> rideHistory = rideService.getUserRideHistory(userId);
            return ResponseEntity.ok(rideHistory);
        } catch (Exception e) {
            log.error("Error getting user ride history", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get driver's ride history
     */
    @GetMapping("/driver/{driverId}/history")
    public ResponseEntity<List<Ride>> getDriverRideHistory(@PathVariable Long driverId) {
        log.info("Getting ride history for driver: {}", driverId);
        
        try {
            List<Ride> rideHistory = rideService.getDriverRideHistory(driverId);
            return ResponseEntity.ok(rideHistory);
        } catch (Exception e) {
            log.error("Error getting driver ride history", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Calculate estimated fare
     */
    @PostMapping("/fare/estimate")
    public ResponseEntity<Map<String, Object>> estimateFare(
            @RequestBody FareEstimateRequest request) {
        
        log.info("Estimating fare for ride: {} to {}", 
            request.getPickupLocation(), request.getDropoffLocation());
        
        try {
            PricingService.FareBreakdown fareBreakdown = pricingService.calculateFareBreakdown(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffLatitude(), request.getDropoffLongitude(),
                request.getVehicleType(), LocalTime.now()
            );
            
            Map<String, Object> response = Map.of(
                "estimatedFare", fareBreakdown.getTotalFare(),
                "estimatedDistance", fareBreakdown.getEstimatedDistance(),
                "estimatedDuration", fareBreakdown.getEstimatedDuration(),
                "baseFare", fareBreakdown.getBaseFare(),
                "distanceFare", fareBreakdown.getDistanceFare(),
                "timeFare", fareBreakdown.getTimeFare(),
                "vehicleMultiplier", fareBreakdown.getVehicleMultiplier(),
                "peakHourMultiplier", fareBreakdown.getPeakHourMultiplier(),
                "surgeMultiplier", fareBreakdown.getSurgeMultiplier()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error estimating fare", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get available drivers in area
     */
    @GetMapping("/drivers/available")
    public ResponseEntity<?> getAvailableDrivers(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusKm,
            @RequestParam(required = false) String vehicleType) {
        
        log.info("Getting available drivers in area: lat={}, lon={}, radius={}km, vehicleType={}", 
            latitude, longitude, radiusKm, vehicleType);
        
        try {
            var drivers = driverMatchingService.getAvailableDriversInArea(
                latitude, longitude, radiusKm, vehicleType);
            return ResponseEntity.ok(drivers);
        } catch (Exception e) {
            log.error("Error getting available drivers", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update driver location
     */
    @PostMapping("/driver/{driverId}/location")
    public ResponseEntity<?> updateDriverLocation(
            @PathVariable Long driverId,
            @RequestBody Map<String, Object> request) {
        
        Double latitude = (Double) request.get("latitude");
        Double longitude = (Double) request.get("longitude");
        Boolean isOnline = (Boolean) request.get("isOnline");
        Boolean isAvailable = (Boolean) request.get("isAvailable");
        
        log.info("Updating driver {} location: lat={}, lon={}, online={}, available={}", 
            driverId, latitude, longitude, isOnline, isAvailable);
        
        try {
            driverMatchingService.updateDriverLocation(driverId, latitude, longitude, isOnline, isAvailable);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating driver location", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Calculate ETA for driver to reach pickup
     */
    @PostMapping("/eta")
    public ResponseEntity<Map<String, Object>> calculateETA(
            @RequestBody EtaRequest request) {
        
        log.info("Calculating ETA from driver location to pickup");
        
        try {
            int etaMinutes = driverMatchingService.calculateETA(
                request.getDriverLatitude(), request.getDriverLongitude(),
                request.getPickupLatitude(), request.getPickupLongitude()
            );
            
            Map<String, Object> response = Map.of(
                "etaMinutes", etaMinutes,
                "etaFormatted", String.format("%d min", etaMinutes)
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating ETA", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // DTOs for requests
    public static class FareEstimateRequest {
        private String pickupLocation;
        private String dropoffLocation;
        private Double pickupLatitude;
        private Double pickupLongitude;
        private Double dropoffLatitude;
        private Double dropoffLongitude;
        private String vehicleType;

        // Getters and setters
        public String getPickupLocation() { return pickupLocation; }
        public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
        
        public String getDropoffLocation() { return dropoffLocation; }
        public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }
        
        public Double getPickupLatitude() { return pickupLatitude; }
        public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }
        
        public Double getPickupLongitude() { return pickupLongitude; }
        public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }
        
        public Double getDropoffLatitude() { return dropoffLatitude; }
        public void setDropoffLatitude(Double dropoffLatitude) { this.dropoffLatitude = dropoffLatitude; }
        
        public Double getDropoffLongitude() { return dropoffLongitude; }
        public void setDropoffLongitude(Double dropoffLongitude) { this.dropoffLongitude = dropoffLongitude; }
        
        public String getVehicleType() { return vehicleType; }
        public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    }

    public static class EtaRequest {
        private Double driverLatitude;
        private Double driverLongitude;
        private Double pickupLatitude;
        private Double pickupLongitude;

        // Getters and setters
        public Double getDriverLatitude() { return driverLatitude; }
        public void setDriverLatitude(Double driverLatitude) { this.driverLatitude = driverLatitude; }
        
        public Double getDriverLongitude() { return driverLongitude; }
        public void setDriverLongitude(Double driverLongitude) { this.driverLongitude = driverLongitude; }
        
        public Double getPickupLatitude() { return pickupLatitude; }
        public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }
        
        public Double getPickupLongitude() { return pickupLongitude; }
        public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }
    }
} 