package com.uberclone.backend.controller;

import com.uberclone.backend.model.DriverLocation;
import com.uberclone.backend.model.Ride;
import com.uberclone.backend.model.User;
import com.uberclone.backend.service.DriverMatchingService;
import com.uberclone.backend.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DriverController {

    private final DriverMatchingService driverMatchingService;
    private final RideService rideService;

    /**
     * Update driver location and availability
     */
    @PostMapping("/{driverId}/location")
    public ResponseEntity<?> updateLocation(
            @PathVariable Long driverId,
            @RequestBody Map<String, Object> request) {
        
        Double latitude = (Double) request.get("latitude");
        Double longitude = (Double) request.get("longitude");
        Boolean isOnline = (Boolean) request.get("isOnline");
        Boolean isAvailable = (Boolean) request.get("isAvailable");
        
        log.info("Driver {} updating location: lat={}, lon={}, online={}, available={}", 
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
     * Get driver's current location
     */
    @GetMapping("/{driverId}/location")
    public ResponseEntity<DriverLocation> getDriverLocation(@PathVariable Long driverId) {
        log.info("Getting location for driver: {}", driverId);
        
        try {
            // This would typically fetch from a repository
            // For now, return a placeholder response
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error getting driver location", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Set driver online/offline status
     */
    @PostMapping("/{driverId}/status")
    public ResponseEntity<?> setDriverStatus(
            @PathVariable Long driverId,
            @RequestBody Map<String, Object> request) {
        
        Boolean isOnline = (Boolean) request.get("isOnline");
        Boolean isAvailable = (Boolean) request.get("isAvailable");
        
        log.info("Driver {} setting status: online={}, available={}", driverId, isOnline, isAvailable);
        
        try {
            // Get current location and update status
            // This would typically fetch current location first
            driverMatchingService.updateDriverLocation(driverId, 0.0, 0.0, isOnline, isAvailable);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error setting driver status", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get available ride requests in driver's area
     */
    @GetMapping("/{driverId}/ride-requests")
    public ResponseEntity<?> getAvailableRideRequests(
            @PathVariable Long driverId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusKm) {
        
        log.info("Getting available ride requests for driver {} in area: lat={}, lon={}, radius={}km", 
            driverId, latitude, longitude, radiusKm);
        
        try {
            // This would typically fetch ride requests from a repository
            // For now, return a placeholder response
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error getting available ride requests", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Accept a ride request
     */
    @PostMapping("/{driverId}/accept-ride/{requestId}")
    public ResponseEntity<Ride> acceptRideRequest(
            @PathVariable Long driverId,
            @PathVariable Long requestId) {
        
        log.info("Driver {} accepting ride request: {}", driverId, requestId);
        
        try {
            // This would typically:
            // 1. Fetch the ride request
            // 2. Validate it's still available
            // 3. Create a ride from the request
            // 4. Update driver availability
            
            // For now, return a placeholder response
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error accepting ride request", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update ride status (driver perspective)
     */
    @PostMapping("/{driverId}/rides/{rideId}/status")
    public ResponseEntity<Ride> updateRideStatus(
            @PathVariable Long driverId,
            @PathVariable Long rideId,
            @RequestBody Map<String, Object> request) {
        
        String newStatus = (String) request.get("status");
        String additionalInfo = (String) request.get("additionalInfo");
        
        log.info("Driver {} updating ride {} status to: {}", driverId, rideId, newStatus);
        
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
     * Get driver's current ride
     */
    @GetMapping("/{driverId}/current-ride")
    public ResponseEntity<Ride> getCurrentRide(@PathVariable Long driverId) {
        log.info("Getting current ride for driver: {}", driverId);
        
        try {
            // This would typically fetch the current active ride for the driver
            // For now, return a placeholder response
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error getting current ride", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get driver's ride history
     */
    @GetMapping("/{driverId}/rides/history")
    public ResponseEntity<List<Ride>> getRideHistory(@PathVariable Long driverId) {
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
     * Rate a passenger after ride completion
     */
    @PostMapping("/{driverId}/rides/{rideId}/rate-passenger")
    public ResponseEntity<?> ratePassenger(
            @PathVariable Long driverId,
            @PathVariable Long rideId,
            @RequestBody Map<String, Object> request) {
        
        Integer rating = (Integer) request.get("rating");
        String review = (String) request.get("review");
        
        log.info("Driver {} rating passenger for ride {}: rating={}", driverId, rideId, rating);
        
        try {
            rideService.rateRide(rideId, rating, review, false); // false = driver rating
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error rating passenger", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get driver earnings summary
     */
    @GetMapping("/{driverId}/earnings")
    public ResponseEntity<Map<String, Object>> getEarnings(
            @PathVariable Long driverId,
            @RequestParam(required = false) String period) {
        
        log.info("Getting earnings for driver: {}, period: {}", driverId, period);
        
        try {
            // This would typically calculate earnings from completed rides
            // For now, return a placeholder response
            Map<String, Object> earnings = Map.of(
                "totalEarnings", 0.0,
                "totalRides", 0,
                "averagePerRide", 0.0,
                "period", period != null ? period : "all"
            );
            
            return ResponseEntity.ok(earnings);
        } catch (Exception e) {
            log.error("Error getting driver earnings", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get driver statistics
     */
    @GetMapping("/{driverId}/stats")
    public ResponseEntity<Map<String, Object>> getDriverStats(@PathVariable Long driverId) {
        log.info("Getting stats for driver: {}", driverId);
        
        try {
            // This would typically calculate various statistics
            // For now, return a placeholder response
            Map<String, Object> stats = Map.of(
                "totalRides", 0,
                "completedRides", 0,
                "cancelledRides", 0,
                "averageRating", 0.0,
                "totalDistance", 0.0,
                "totalEarnings", 0.0
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting driver stats", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update driver profile information
     */
    @PutMapping("/{driverId}/profile")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long driverId,
            @RequestBody Map<String, Object> request) {
        
        log.info("Driver {} updating profile", driverId);
        
        try {
            // This would typically update driver profile information
            // For now, return a placeholder response
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating driver profile", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get driver profile
     */
    @GetMapping("/{driverId}/profile")
    public ResponseEntity<User> getDriverProfile(@PathVariable Long driverId) {
        log.info("Getting profile for driver: {}", driverId);
        
        try {
            // This would typically fetch driver profile information
            // For now, return a placeholder response
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error getting driver profile", e);
            return ResponseEntity.badRequest().build();
        }
    }
} 