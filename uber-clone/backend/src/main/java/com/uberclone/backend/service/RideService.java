package com.uberclone.backend.service;

import com.uberclone.backend.model.*;
import com.uberclone.backend.repository.RideRepository;
import com.uberclone.backend.repository.RideRequestRepository;
import com.uberclone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;
    private final UserRepository userRepository;
    private final DriverMatchingService driverMatchingService;
    private final PricingService pricingService;
    private final GeohashService geohashService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a new ride request
     */
    @Transactional
    public RideRequest createRideRequest(RideRequestRequest request) {
        log.info("Creating ride request for user: {}", request.getUserId());

        // Validate user
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate geohashes for pickup and dropoff locations
        String pickupGeohash = geohashService.encode(request.getPickupLatitude(), request.getPickupLongitude());
        String dropoffGeohash = geohashService.encode(request.getDropoffLatitude(), request.getDropoffLongitude());

        // Calculate estimated fare
        PricingService.FareBreakdown fareBreakdown = pricingService.calculateFareBreakdown(
            request.getPickupLatitude(), request.getPickupLongitude(),
            request.getDropoffLatitude(), request.getDropoffLongitude(),
            request.getVehicleType(), LocalTime.now()
        );

        // Create ride request
        RideRequest rideRequest = RideRequest.builder()
            .user(user)
            .pickupLocation(request.getPickupLocation())
            .dropoffLocation(request.getDropoffLocation())
            .pickupLatitude(request.getPickupLatitude())
            .pickupLongitude(request.getPickupLongitude())
            .dropoffLatitude(request.getDropoffLatitude())
            .dropoffLongitude(request.getDropoffLongitude())
            .pickupGeohash(pickupGeohash)
            .dropoffGeohash(dropoffGeohash)
            .vehicleType(request.getVehicleType())
            .estimatedDuration(fareBreakdown.getEstimatedDuration())
            .estimatedDistance(BigDecimal.valueOf(fareBreakdown.getEstimatedDistance()))
            .estimatedFare(fareBreakdown.getTotalFare())
            .passengerCount(request.getPassengerCount())
            .paymentMethod(request.getPaymentMethod())
            .specialInstructions(request.getSpecialInstructions())
            .status(RideRequest.Status.PENDING)
            .build();

        RideRequest savedRequest = rideRequestRepository.save(rideRequest);

        // Start driver search process asynchronously
        CompletableFuture.runAsync(() -> searchForDriver(savedRequest));

        // Send real-time update to user
        sendRideRequestUpdate(savedRequest);

        return savedRequest;
    }

    /**
     * Search for available drivers
     */
    private void searchForDriver(RideRequest rideRequest) {
        log.info("Searching for drivers for ride request: {}", rideRequest.getId());

        try {
            // Update status to searching
            rideRequest.setStatus(RideRequest.Status.SEARCHING_DRIVER);
            rideRequestRepository.save(rideRequest);
            sendRideRequestUpdate(rideRequest);

            // Try to find and assign a driver
            Optional<User> assignedDriver = driverMatchingService.assignDriverToRide(rideRequest);

            if (assignedDriver.isPresent()) {
                // Driver found, create ride
                createRideFromRequest(rideRequest, assignedDriver.get());
            } else {
                // No driver found, mark as expired
                handleNoDriverFound(rideRequest);
            }

        } catch (Exception e) {
            log.error("Error searching for driver for ride request: {}", rideRequest.getId(), e);
            handleNoDriverFound(rideRequest);
        }
    }

    /**
     * Create ride from ride request
     */
    @Transactional
    public Ride createRideFromRequest(RideRequest rideRequest, User driver) {
        log.info("Creating ride from request: {} with driver: {}", rideRequest.getId(), driver.getId());

        // Update ride request status
        rideRequest.setStatus(RideRequest.Status.ACCEPTED);
        rideRequestRepository.save(rideRequest);

        // Create ride
        Ride ride = Ride.builder()
            .user(rideRequest.getUser())
            .driver(driver)
            .pickupLocation(rideRequest.getPickupLocation())
            .dropoffLocation(rideRequest.getDropoffLocation())
            .pickupLatitude(rideRequest.getPickupLatitude())
            .pickupLongitude(rideRequest.getPickupLongitude())
            .dropoffLatitude(rideRequest.getDropoffLatitude())
            .dropoffLongitude(rideRequest.getDropoffLongitude())
            .baseFare(rideRequest.getEstimatedFare().multiply(new BigDecimal("0.3")))
            .distanceFare(rideRequest.getEstimatedFare().multiply(new BigDecimal("0.5")))
            .timeFare(rideRequest.getEstimatedFare().multiply(new BigDecimal("0.2")))
            .totalFare(rideRequest.getEstimatedFare())
            .distance(rideRequest.getEstimatedDistance())
            .estimatedDuration(rideRequest.getEstimatedDuration())
            .status(Ride.Status.DRIVER_ASSIGNED)
            .requestedAt(rideRequest.getRequestedAt())
            .acceptedAt(LocalDateTime.now())
            .paymentMethod(rideRequest.getPaymentMethod())
            .paymentStatus("PENDING")
            .build();

        Ride savedRide = rideRepository.save(ride);

        // Send notifications
        notificationService.notifyDriverAssigned(rideRequest.getUser(), driver, savedRide);
        notificationService.notifyRideAccepted(rideRequest.getUser(), driver, savedRide);

        // Send real-time updates
        sendRideUpdate(savedRide);

        return savedRide;
    }

    /**
     * Handle case when no driver is found
     */
    private void handleNoDriverFound(RideRequest rideRequest) {
        log.warn("No driver found for ride request: {}", rideRequest.getId());
        
        rideRequest.setStatus(RideRequest.Status.EXPIRED);
        rideRequestRepository.save(rideRequest);

        // Notify user
        notificationService.notifyNoDriverFound(rideRequest.getUser());
        sendRideRequestUpdate(rideRequest);
    }

    /**
     * Update ride status
     */
    @Transactional
    public Ride updateRideStatus(Long rideId, Ride.Status newStatus, String additionalInfo) {
        log.info("Updating ride {} status to: {}", rideId, newStatus);

        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));

        Ride.Status oldStatus = ride.getStatus();
        ride.setStatus(newStatus);

        // Update timestamps based on status
        switch (newStatus) {
            case REQUESTED:
                // No specific timestamp for this status
                break;
            case SEARCHING_DRIVER:
                // No specific timestamp for this status
                break;
            case DRIVER_ASSIGNED:
                // No specific timestamp for this status
                break;
            case DRIVER_ARRIVING:
                // No specific timestamp for this status
                break;
            case DRIVER_ARRIVED:
                ride.setStartedAt(LocalDateTime.now());
                break;
            case IN_PROGRESS:
                if (ride.getStartedAt() == null) {
                    ride.setStartedAt(LocalDateTime.now());
                }
                break;
            case COMPLETED:
                ride.setCompletedAt(LocalDateTime.now());
                calculateActualRideMetrics(ride);
                break;
            case CANCELLED:
                ride.setCancelledAt(LocalDateTime.now());
                if (additionalInfo != null) {
                    ride.setCancellationReason(additionalInfo);
                }
                // Release driver
                if (ride.getDriver() != null) {
                    driverMatchingService.releaseDriver(ride.getDriver());
                }
                break;
        }

        Ride updatedRide = rideRepository.save(ride);

        // Send real-time updates
        sendRideUpdate(updatedRide);

        // Send notifications based on status change
        sendStatusChangeNotifications(updatedRide, oldStatus, newStatus);

        return updatedRide;
    }

    /**
     * Calculate actual ride metrics
     */
    private void calculateActualRideMetrics(Ride ride) {
        if (ride.getStartedAt() != null && ride.getCompletedAt() != null) {
            long durationMinutes = java.time.Duration.between(ride.getStartedAt(), ride.getCompletedAt()).toMinutes();
            ride.setActualDuration((int) durationMinutes);
        }
    }

    /**
     * Send status change notifications
     */
    private void sendStatusChangeNotifications(Ride ride, Ride.Status oldStatus, Ride.Status newStatus) {
        switch (newStatus) {
            case REQUESTED:
                // No specific notification for this status
                break;
            case SEARCHING_DRIVER:
                // No specific notification for this status
                break;
            case DRIVER_ASSIGNED:
                // No specific notification for this status
                break;
            case DRIVER_ARRIVING:
                notificationService.notifyDriverArriving(ride.getUser(), ride.getDriver(), ride);
                break;
            case DRIVER_ARRIVED:
                notificationService.notifyDriverArrived(ride.getUser(), ride.getDriver(), ride);
                break;
            case IN_PROGRESS:
                notificationService.notifyRideStarted(ride.getUser(), ride.getDriver(), ride);
                break;
            case COMPLETED:
                notificationService.notifyRideCompleted(ride.getUser(), ride.getDriver(), ride);
                // Request ratings
                requestRideRating(ride);
                break;
            case CANCELLED:
                notificationService.notifyRideCancelled(ride.getUser(), ride.getDriver(), ride);
                break;
        }
    }

    /**
     * Request ride rating
     */
    private void requestRideRating(Ride ride) {
        // Send rating request notification
        notificationService.requestRideRating(ride.getUser(), ride);
        if (ride.getDriver() != null) {
            notificationService.requestDriverRating(ride.getDriver(), ride);
        }
    }

    /**
     * Rate a ride
     */
    @Transactional
    public void rateRide(Long rideId, Integer rating, String review, boolean isUserRating) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (isUserRating) {
            ride.setUserRating(rating);
            ride.setUserReview(review);
        } else {
            ride.setDriverRating(rating);
            ride.setDriverReview(review);
        }

        rideRepository.save(ride);

        // Update user rating if driver rating
        if (!isUserRating && ride.getDriver() != null) {
            updateUserRating(ride.getDriver(), rating);
        }
    }

    /**
     * Update user rating
     */
    private void updateUserRating(User user, Integer newRating) {
        if (user.getRating() == null) {
            user.setRating(0.0);
            user.setRatingCount(0);
        }

        double currentTotal = user.getRating() * user.getRatingCount();
        user.setRatingCount(user.getRatingCount() + 1);
        user.setRating((currentTotal + newRating) / user.getRatingCount());

        userRepository.save(user);
    }

    /**
     * Get user's ride history
     */
    public List<Ride> getUserRideHistory(Long userId) {
        return rideRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get driver's ride history
     */
    public List<Ride> getDriverRideHistory(Long driverId) {
        return rideRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
    }

    /**
     * Cancel ride
     */
    @Transactional
    public Ride cancelRide(Long rideId, String reason, boolean cancelledByUser) {
        log.info("Cancelling ride: {} by {}", rideId, cancelledByUser ? "user" : "driver");

        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getStatus() == Ride.Status.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed ride");
        }

        Ride.CancelledBy cancelledBy = cancelledByUser ? 
            Ride.CancelledBy.USER : Ride.CancelledBy.DRIVER;

        ride.setCancelledBy(cancelledBy);
        ride.setCancellationReason(reason);

        return updateRideStatus(rideId, Ride.Status.CANCELLED, reason);
    }

    /**
     * Send real-time ride request update
     */
    private void sendRideRequestUpdate(RideRequest rideRequest) {
        String destination = "/user/" + rideRequest.getUser().getId() + "/ride-request";
        messagingTemplate.convertAndSend(destination, rideRequest);
    }

    /**
     * Send real-time ride update
     */
    private void sendRideUpdate(Ride ride) {
        String userDestination = "/user/" + ride.getUser().getId() + "/ride";
        String driverDestination = "/user/" + ride.getDriver().getId() + "/ride";
        
        messagingTemplate.convertAndSend(userDestination, ride);
        if (ride.getDriver() != null) {
            messagingTemplate.convertAndSend(driverDestination, ride);
        }
    }

    // DTO for ride request creation
    public static class RideRequestRequest {
        private Long userId;
        private String pickupLocation;
        private String dropoffLocation;
        private Double pickupLatitude;
        private Double pickupLongitude;
        private Double dropoffLatitude;
        private Double dropoffLongitude;
        private String vehicleType;
        private Integer passengerCount;
        private String paymentMethod;
        private String specialInstructions;

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
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
        
        public Integer getPassengerCount() { return passengerCount; }
        public void setPassengerCount(Integer passengerCount) { this.passengerCount = passengerCount; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public String getSpecialInstructions() { return specialInstructions; }
        public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    }
} 