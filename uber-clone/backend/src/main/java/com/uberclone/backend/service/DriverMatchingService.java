package com.uberclone.backend.service;

import com.uberclone.backend.model.DriverLocation;
import com.uberclone.backend.model.RideRequest;
import com.uberclone.backend.model.User;
import com.uberclone.backend.repository.DriverLocationRepository;
import com.uberclone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverMatchingService {

    private final DriverLocationRepository driverLocationRepository;
    private final UserRepository userRepository;
    private final GeohashService geohashService;

    /**
     * Find available drivers for a ride request using geohashing
     */
    public List<DriverLocation> findAvailableDrivers(RideRequest rideRequest, double searchRadiusKm) {
        log.info("Finding drivers for ride request: {}", rideRequest.getId());
        
        // Get geohash prefixes for the search radius
        List<String> geohashPrefixes = geohashService.getGeohashPrefixes(
            rideRequest.getPickupLatitude(), 
            rideRequest.getPickupLongitude(), 
            searchRadiusKm
        );

        // Find drivers in the geohash area
        List<DriverLocation> nearbyDrivers = driverLocationRepository
            .findByGeohashStartingWithAndIsOnlineTrueAndIsAvailableTrue(geohashPrefixes.get(0));

        if (nearbyDrivers.isEmpty()) {
            // Try with broader geohash prefixes
            for (String prefix : geohashPrefixes) {
                nearbyDrivers = driverLocationRepository
                    .findByGeohashStartingWithAndIsOnlineTrueAndIsAvailableTrue(prefix);
                if (!nearbyDrivers.isEmpty()) {
                    break;
                }
            }
        }

        // Filter and rank drivers based on distance and rating
        return rankDriversByProximityAndRating(nearbyDrivers, rideRequest);
    }

    /**
     * Rank drivers by proximity and rating
     */
    private List<DriverLocation> rankDriversByProximityAndRating(
            List<DriverLocation> drivers, RideRequest rideRequest) {
        
        return drivers.stream()
            .map(driver -> {
                double distance = geohashService.calculateDistance(
                    driver.getLatitude(), driver.getLongitude(),
                    rideRequest.getPickupLatitude(), rideRequest.getPickupLongitude()
                );
                
                // Calculate score based on distance and rating
                double score = calculateDriverScore(driver, distance);
                
                return new AbstractMap.SimpleEntry<>(driver, score);
            })
            .sorted(Map.Entry.<DriverLocation, Double>comparingByValue().reversed())
            .limit(10) // Return top 10 drivers
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Calculate driver score based on distance and rating
     */
    private double calculateDriverScore(DriverLocation driver, double distance) {
        User driverUser = driver.getDriver();
        double rating = driverUser.getRating() != null ? driverUser.getRating() : 0.0;
        
        // Distance penalty (closer is better)
        double distanceScore = Math.max(0, 10 - distance);
        
        // Rating score (0-5 scale)
        double ratingScore = rating * 2;
        
        // Combine scores with weights
        return (distanceScore * 0.7) + (ratingScore * 0.3);
    }

    /**
     * Assign driver to ride request
     */
    public Optional<User> assignDriverToRide(RideRequest rideRequest) {
        log.info("Assigning driver to ride request: {}", rideRequest.getId());
        
        // Find available drivers within 5km radius
        List<DriverLocation> availableDrivers = findAvailableDrivers(rideRequest, 5.0);
        
        if (availableDrivers.isEmpty()) {
            log.warn("No available drivers found for ride request: {}", rideRequest.getId());
            return Optional.empty();
        }

        // Try to assign the best driver
        for (DriverLocation driverLocation : availableDrivers) {
            if (tryAssignDriver(driverLocation.getDriver(), rideRequest)) {
                log.info("Driver {} assigned to ride request {}", 
                    driverLocation.getDriver().getId(), rideRequest.getId());
                return Optional.of(driverLocation.getDriver());
            }
        }

        log.warn("Failed to assign any driver to ride request: {}", rideRequest.getId());
        return Optional.empty();
    }

    /**
     * Try to assign a specific driver to a ride request
     */
    private boolean tryAssignDriver(User driver, RideRequest rideRequest) {
        // Check if driver is still available
        Optional<DriverLocation> currentLocation = driverLocationRepository
            .findByDriverId(driver.getId());
        
        if (currentLocation.isEmpty() || 
            !currentLocation.get().getIsAvailable() || 
            !currentLocation.get().getIsOnline()) {
            return false;
        }

        // Update driver availability
        DriverLocation location = currentLocation.get();
        location.setIsAvailable(false);
        driverLocationRepository.save(location);

        // Update ride request status
        rideRequest.setStatus(RideRequest.Status.DRIVER_FOUND);
        // Note: This would typically be saved through a repository
        
        return true;
    }

    /**
     * Release driver from ride assignment
     */
    public void releaseDriver(User driver) {
        log.info("Releasing driver: {}", driver.getId());
        
        Optional<DriverLocation> location = driverLocationRepository
            .findByDriverId(driver.getId());
        
        if (location.isPresent()) {
            DriverLocation driverLocation = location.get();
            driverLocation.setIsAvailable(true);
            driverLocationRepository.save(driverLocation);
        }
    }

    /**
     * Update driver location and availability
     */
    public void updateDriverLocation(Long driverId, double latitude, double longitude, 
                                   boolean isOnline, boolean isAvailable) {
        log.debug("Updating driver location: driverId={}, lat={}, lon={}, online={}, available={}", 
            driverId, latitude, longitude, isOnline, isAvailable);
        
        String geohash = geohashService.encode(latitude, longitude);
        
        Optional<DriverLocation> existingLocation = driverLocationRepository
            .findByDriverId(driverId);
        
        if (existingLocation.isPresent()) {
            DriverLocation location = existingLocation.get();
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setGeohash(geohash);
            location.setIsOnline(isOnline);
            location.setIsAvailable(isAvailable);
            location.setTimestamp(java.time.LocalDateTime.now());
            driverLocationRepository.save(location);
        } else {
            // Create new location record
            User driver = userRepository.findById(driverId).orElse(null);
            if (driver != null) {
                DriverLocation newLocation = DriverLocation.builder()
                    .driver(driver)
                    .latitude(latitude)
                    .longitude(longitude)
                    .geohash(geohash)
                    .isOnline(isOnline)
                    .isAvailable(isAvailable)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
                driverLocationRepository.save(newLocation);
            }
        }
    }

    /**
     * Get all available drivers in a specific area
     */
    public List<DriverLocation> getAvailableDriversInArea(double latitude, double longitude, 
                                                        double radiusKm, String vehicleType) {
        List<String> geohashPrefixes = geohashService.getGeohashPrefixes(latitude, longitude, radiusKm);
        
        List<DriverLocation> drivers = new ArrayList<>();
        
        for (String prefix : geohashPrefixes) {
            List<DriverLocation> prefixDrivers;
            if (vehicleType != null && !vehicleType.isEmpty()) {
                prefixDrivers = driverLocationRepository
                    .findByGeohashStartingWithAndIsOnlineTrueAndIsAvailableTrueAndVehicleType(prefix, vehicleType);
            } else {
                prefixDrivers = driverLocationRepository
                    .findByGeohashStartingWithAndIsOnlineTrueAndIsAvailableTrue(prefix);
            }
            drivers.addAll(prefixDrivers);
        }
        
        return drivers.stream()
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Calculate ETA for driver to reach pickup location
     */
    public int calculateETA(double driverLat, double driverLon, 
                           double pickupLat, double pickupLon) {
        double distance = geohashService.calculateDistance(driverLat, driverLon, pickupLat, pickupLon);
        
        // Assume average speed of 30 km/h in city traffic
        double averageSpeedKmh = 30.0;
        double timeHours = distance / averageSpeedKmh;
        
        return (int) Math.ceil(timeHours * 60); // Convert to minutes
    }
}
