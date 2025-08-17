package com.uberclone.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    // Base pricing constants
    private static final BigDecimal BASE_FARE = new BigDecimal("2.50");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("1.50");
    private static final BigDecimal PER_MINUTE_RATE = new BigDecimal("0.30");
    
    // Surge pricing constants
    private static final BigDecimal PEAK_HOUR_MULTIPLIER = new BigDecimal("1.2");
    
    // Vehicle type multipliers
    private static final BigDecimal STANDARD_MULTIPLIER = new BigDecimal("1.0");
    private static final BigDecimal COMFORT_MULTIPLIER = new BigDecimal("1.3");
    private static final BigDecimal PREMIUM_MULTIPLIER = new BigDecimal("1.8");
    private static final BigDecimal POOL_MULTIPLIER = new BigDecimal("0.7");

    private final GeohashService geohashService;

    /**
     * Calculate total fare for a ride
     */
    public BigDecimal calculateFare(double pickupLat, double pickupLon, 
                                   double dropoffLat, double dropoffLon,
                                   String vehicleType, LocalTime requestTime) {
        
        // Calculate distance
        double distanceKm = geohashService.calculateDistance(pickupLat, pickupLon, dropoffLat, dropoffLon);
        
        // Calculate estimated time (assuming 30 km/h average speed)
        int estimatedMinutes = (int) Math.ceil(distanceKm * 2); // 2 minutes per km
        
        // Calculate base components
        BigDecimal distanceFare = PER_KM_RATE.multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal timeFare = PER_MINUTE_RATE.multiply(BigDecimal.valueOf(estimatedMinutes));
        
        // Apply vehicle type multiplier
        BigDecimal vehicleMultiplier = getVehicleMultiplier(vehicleType);
        BigDecimal subtotal = BASE_FARE.add(distanceFare).add(timeFare);
        BigDecimal vehicleAdjustedFare = subtotal.multiply(vehicleMultiplier);
        
        // Apply peak hour multiplier
        BigDecimal peakAdjustedFare = applyPeakHourMultiplier(vehicleAdjustedFare, requestTime);
        
        // Apply surge pricing if applicable
        BigDecimal finalFare = applySurgePricing(peakAdjustedFare, pickupLat, pickupLon, requestTime);
        
        // Round to 2 decimal places
        return finalFare.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get vehicle type multiplier
     */
    private BigDecimal getVehicleMultiplier(String vehicleType) {
        if (vehicleType == null) return STANDARD_MULTIPLIER;
        
        return switch (vehicleType.toUpperCase()) {
            case "COMFORT" -> COMFORT_MULTIPLIER;
            case "PREMIUM" -> PREMIUM_MULTIPLIER;
            case "POOL" -> POOL_MULTIPLIER;
            default -> STANDARD_MULTIPLIER;
        };
    }

    /**
     * Apply peak hour multiplier
     */
    private BigDecimal applyPeakHourMultiplier(BigDecimal fare, LocalTime requestTime) {
        if (isPeakHour(requestTime)) {
            return fare.multiply(PEAK_HOUR_MULTIPLIER);
        }
        return fare;
    }

    /**
     * Check if request time is during peak hours
     */
    private boolean isPeakHour(LocalTime time) {
        // Morning peak: 7-9 AM
        LocalTime morningStart = LocalTime.of(7, 0);
        LocalTime morningEnd = LocalTime.of(9, 0);
        
        // Evening peak: 5-7 PM
        LocalTime eveningStart = LocalTime.of(17, 0);
        LocalTime eveningEnd = LocalTime.of(19, 0);
        
        return (time.isAfter(morningStart) && time.isBefore(morningEnd)) ||
               (time.isAfter(eveningStart) && time.isBefore(eveningEnd));
    }

    /**
     * Apply surge pricing based on demand and location
     */
    private BigDecimal applySurgePricing(BigDecimal fare, double latitude, double longitude, LocalTime requestTime) {
        // This is a simplified surge pricing algorithm
        // In a real implementation, this would consider:
        // - Current demand in the area
        // - Number of available drivers
        // - Historical demand patterns
        // - Special events or weather conditions
        
        BigDecimal surgeMultiplier = calculateSurgeMultiplier(latitude, longitude, requestTime);
        
        if (surgeMultiplier.compareTo(BigDecimal.ONE) > 0) {
            log.info("Applying surge pricing multiplier: {}x", surgeMultiplier);
        }
        
        return fare.multiply(surgeMultiplier);
    }

    /**
     * Calculate surge pricing multiplier
     */
    private BigDecimal calculateSurgeMultiplier(double latitude, double longitude, LocalTime requestTime) {
        // Simplified surge calculation
        // In reality, this would query a demand analysis service
        
        // Weekend nights typically have higher demand
        if (isWeekendNight(requestTime)) {
            return new BigDecimal("1.5");
        }
        
        // Business districts during business hours
        if (isBusinessDistrict(latitude, longitude) && isBusinessHours(requestTime)) {
            return new BigDecimal("1.3");
        }
        
        return BigDecimal.ONE;
    }

    /**
     * Check if it's weekend night (higher demand)
     */
    private boolean isWeekendNight(LocalTime time) {
        // Weekend nights: 10 PM - 2 AM
        LocalTime nightStart = LocalTime.of(22, 0);
        LocalTime nightEnd = LocalTime.of(2, 0);
        
        return time.isAfter(nightStart) || time.isBefore(nightEnd);
    }

    /**
     * Check if location is in business district
     */
    private boolean isBusinessDistrict(double latitude, double longitude) {
        // Simplified business district check
        // In reality, this would query a geospatial database
        
        // Example: Downtown area coordinates (this would be configurable)
        double downtownLat = 40.7589; // Example: NYC coordinates
        double downtownLon = -73.9851;
        double radiusKm = 2.0;
        
        double distance = geohashService.calculateDistance(latitude, longitude, downtownLat, downtownLon);
        return distance <= radiusKm;
    }

    /**
     * Check if it's business hours
     */
    private boolean isBusinessHours(LocalTime time) {
        LocalTime businessStart = LocalTime.of(8, 0);
        LocalTime businessEnd = LocalTime.of(18, 0);
        
        return time.isAfter(businessStart) && time.isBefore(businessEnd);
    }

    /**
     * Calculate fare breakdown
     */
    public FareBreakdown calculateFareBreakdown(double pickupLat, double pickupLon,
                                               double dropoffLat, double dropoffLon,
                                               String vehicleType, LocalTime requestTime) {
        
        double distanceKm = geohashService.calculateDistance(pickupLat, pickupLon, dropoffLat, dropoffLon);
        int estimatedMinutes = (int) Math.ceil(distanceKm * 2);
        
        BigDecimal distanceFare = PER_KM_RATE.multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal timeFare = PER_MINUTE_RATE.multiply(BigDecimal.valueOf(estimatedMinutes));
        BigDecimal vehicleMultiplier = getVehicleMultiplier(vehicleType);
        
        BigDecimal subtotal = BASE_FARE.add(distanceFare).add(timeFare);
        BigDecimal vehicleAdjustedFare = subtotal.multiply(vehicleMultiplier);
        BigDecimal peakAdjustedFare = applyPeakHourMultiplier(vehicleAdjustedFare, requestTime);
        BigDecimal finalFare = applySurgePricing(peakAdjustedFare, pickupLat, pickupLon, requestTime);
        
        return FareBreakdown.builder()
            .baseFare(BASE_FARE)
            .distanceFare(distanceFare)
            .timeFare(timeFare)
            .vehicleMultiplier(vehicleMultiplier)
            .peakHourMultiplier(isPeakHour(requestTime) ? PEAK_HOUR_MULTIPLIER : BigDecimal.ONE)
            .surgeMultiplier(calculateSurgeMultiplier(pickupLat, pickupLon, requestTime))
            .totalFare(finalFare.setScale(2, RoundingMode.HALF_UP))
            .estimatedDistance(distanceKm)
            .estimatedDuration(estimatedMinutes)
            .build();
    }

    /**
     * Fare breakdown data class
     */
    public static class FareBreakdown {
        private BigDecimal baseFare;
        private BigDecimal distanceFare;
        private BigDecimal timeFare;
        private BigDecimal vehicleMultiplier;
        private BigDecimal peakHourMultiplier;
        private BigDecimal surgeMultiplier;
        private BigDecimal totalFare;
        private double estimatedDistance;
        private int estimatedDuration;
        
        // Builder methods
        public static FareBreakdownBuilder builder() {
            return new FareBreakdownBuilder();
        }
        
        public static class FareBreakdownBuilder {
            private FareBreakdown breakdown = new FareBreakdown();
            
            public FareBreakdownBuilder baseFare(BigDecimal baseFare) {
                breakdown.baseFare = baseFare;
                return this;
            }
            
            public FareBreakdownBuilder distanceFare(BigDecimal distanceFare) {
                breakdown.distanceFare = distanceFare;
                return this;
            }
            
            public FareBreakdownBuilder timeFare(BigDecimal timeFare) {
                breakdown.timeFare = timeFare;
                return this;
            }
            
            public FareBreakdownBuilder vehicleMultiplier(BigDecimal vehicleMultiplier) {
                breakdown.vehicleMultiplier = vehicleMultiplier;
                return this;
            }
            
            public FareBreakdownBuilder peakHourMultiplier(BigDecimal peakHourMultiplier) {
                breakdown.peakHourMultiplier = peakHourMultiplier;
                return this;
            }
            
            public FareBreakdownBuilder surgeMultiplier(BigDecimal surgeMultiplier) {
                breakdown.surgeMultiplier = surgeMultiplier;
                return this;
            }
            
            public FareBreakdownBuilder totalFare(BigDecimal totalFare) {
                breakdown.totalFare = totalFare;
                return this;
            }
            
            public FareBreakdownBuilder estimatedDistance(double estimatedDistance) {
                breakdown.estimatedDistance = estimatedDistance;
                return this;
            }
            
            public FareBreakdownBuilder estimatedDuration(int estimatedDuration) {
                breakdown.estimatedDuration = estimatedDuration;
                return this;
            }
            
            public FareBreakdown build() {
                return breakdown;
            }
        }
        
        // Getters
        public BigDecimal getBaseFare() { return baseFare; }
        public BigDecimal getDistanceFare() { return distanceFare; }
        public BigDecimal getTimeFare() { return timeFare; }
        public BigDecimal getVehicleMultiplier() { return vehicleMultiplier; }
        public BigDecimal getPeakHourMultiplier() { return peakHourMultiplier; }
        public BigDecimal getSurgeMultiplier() { return surgeMultiplier; }
        public BigDecimal getTotalFare() { return totalFare; }
        public double getEstimatedDistance() { return estimatedDistance; }
        public int getEstimatedDuration() { return estimatedDuration; }
    }
}
