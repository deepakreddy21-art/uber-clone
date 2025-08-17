package com.uberclone.backend.websocket;

import com.uberclone.backend.model.DriverLocation;
import com.uberclone.backend.model.Ride;
import com.uberclone.backend.model.RideRequest;
import com.uberclone.backend.service.DriverMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RideStatusWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle ride request updates
     */
    @MessageMapping("/ride-request")
    @SendTo("/topic/ride-request")
    public RideRequest handleRideRequest(@Payload RideRequest rideRequest) {
        log.info("Received ride request via WebSocket: {}", rideRequest.getId());
        return rideRequest;
    }

    /**
     * Handle ride status updates
     */
    @MessageMapping("/ride-status")
    @SendTo("/topic/ride-status")
    public Ride handleRideStatus(@Payload Ride ride) {
        log.info("Received ride status update via WebSocket: {}", ride.getId());
        return ride;
    }

    /**
     * Handle driver location updates
     */
    @MessageMapping("/driver-location")
    @SendTo("/topic/driver-location")
    public DriverLocation handleDriverLocation(@Payload DriverLocation driverLocation) {
        log.info("Received driver location update via WebSocket: {}", driverLocation.getDriver().getId());
        return driverLocation;
    }

    /**
     * Handle user joining ride tracking
     */
    @MessageMapping("/join-ride-tracking")
    public void handleJoinRideTracking(@Payload Map<String, Object> request, 
                                     SimpMessageHeaderAccessor headerAccessor) {
        String rideId = (String) request.get("rideId");
        String userId = (String) request.get("userId");
        
        log.info("User {} joining ride tracking for ride: {}", userId, rideId);
        
        // Add user to ride tracking room
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("rideId", rideId);
            headerAccessor.getSessionAttributes().put("userId", userId);
        }
    }

    /**
     * Handle driver joining ride tracking
     */
    @MessageMapping("/join-driver-tracking")
    public void handleJoinDriverTracking(@Payload Map<String, Object> request, 
                                       SimpMessageHeaderAccessor headerAccessor) {
        String rideId = (String) request.get("rideId");
        String driverId = (String) request.get("driverId");
        
        log.info("Driver {} joining ride tracking for ride: {}", driverId, rideId);
        
        // Add driver to ride tracking room
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("rideId", rideId);
            headerAccessor.getSessionAttributes().put("driverId", driverId);
        }
    }

    /**
     * Handle user leaving ride tracking
     */
    @MessageMapping("/leave-ride-tracking")
    public void handleLeaveRideTracking(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() != null) {
            String rideId = (String) headerAccessor.getSessionAttributes().get("rideId");
            String userId = (String) headerAccessor.getSessionAttributes().get("userId");
            
            log.info("User {} leaving ride tracking for ride: {}", userId, rideId);
            
            // Clean up session attributes
            headerAccessor.getSessionAttributes().remove("rideId");
            headerAccessor.getSessionAttributes().remove("userId");
        }
    }

    /**
     * Handle driver leaving ride tracking
     */
    @MessageMapping("/leave-driver-tracking")
    public void handleLeaveDriverTracking(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() != null) {
            String rideId = (String) headerAccessor.getSessionAttributes().get("rideId");
            String driverId = (String) headerAccessor.getSessionAttributes().get("driverId");
            
            log.info("Driver {} leaving ride tracking for ride: {}", driverId, rideId);
            
            // Clean up session attributes
            headerAccessor.getSessionAttributes().remove("rideId");
            headerAccessor.getSessionAttributes().remove("driverId");
        }
    }

    /**
     * Send ride request update to specific user
     */
    public void sendRideRequestUpdate(Long userId, RideRequest rideRequest) {
        String destination = "/user/" + userId + "/ride-request";
        messagingTemplate.convertAndSend(destination, rideRequest);
        log.debug("Sent ride request update to user {}: {}", userId, rideRequest.getId());
    }

    /**
     * Send ride update to specific user
     */
    public void sendRideUpdate(Long userId, Ride ride) {
        String destination = "/user/" + userId + "/ride";
        messagingTemplate.convertAndSend(destination, ride);
        log.debug("Sent ride update to user {}: {}", userId, ride.getId());
    }

    /**
     * Send ride update to specific driver
     */
    public void sendRideUpdateToDriver(Long driverId, Ride ride) {
        String destination = "/user/" + driverId + "/ride";
        messagingTemplate.convertAndSend(destination, ride);
        log.debug("Sent ride update to driver {}: {}", driverId, ride.getId());
    }

    /**
     * Send driver location update to ride participants
     */
    public void sendDriverLocationUpdate(Long rideId, DriverLocation driverLocation) {
        String destination = "/topic/ride/" + rideId + "/driver-location";
        messagingTemplate.convertAndSend(destination, driverLocation);
        if (driverLocation.getDriver() != null) {
            log.debug("Sent driver location update for ride {}: {}", rideId, driverLocation.getDriver().getId());
        } else {
            log.debug("Sent driver location update for ride {}: driver is null", rideId);
        }
    }

    /**
     * Send ride status update to all participants
     */
    public void sendRideStatusUpdate(Long rideId, Ride ride) {
        String destination = "/topic/ride/" + rideId + "/status";
        messagingTemplate.convertAndSend(destination, ride);
        if (ride.getStatus() != null) {
            log.debug("Sent ride status update for ride {}: {}", rideId, ride.getStatus());
        } else {
            log.debug("Sent ride status update for ride {}: status is null", rideId);
        }
    }

    /**
     * Send ETA update to user
     */
    public void sendEtaUpdate(Long userId, Long rideId, int etaMinutes) {
        Map<String, Object> etaUpdate = Map.of(
            "rideId", rideId,
            "etaMinutes", etaMinutes,
            "etaFormatted", String.format("%d min", etaMinutes),
            "timestamp", System.currentTimeMillis()
        );
        
        String destination = "/user/" + userId + "/eta";
        messagingTemplate.convertAndSend(destination, etaUpdate);
        log.debug("Sent ETA update to user {} for ride {}: {} minutes", userId, rideId, etaMinutes);
    }

    /**
     * Send driver arrival notification
     */
    public void sendDriverArrivalNotification(Long userId, Long rideId, String driverName) {
        Map<String, Object> arrivalNotification = Map.of(
            "rideId", rideId,
            "driverName", driverName,
            "message", "Your driver has arrived!",
            "timestamp", System.currentTimeMillis()
        );
        
        String destination = "/user/" + userId + "/notifications";
        messagingTemplate.convertAndSend(destination, arrivalNotification);
        log.debug("Sent driver arrival notification to user {} for ride {}", userId, rideId);
    }

    /**
     * Send ride completion notification
     */
    public void sendRideCompletionNotification(Long userId, Long rideId, double totalFare) {
        Map<String, Object> completionNotification = Map.of(
            "rideId", rideId,
            "totalFare", totalFare,
            "message", "Your ride has been completed!",
            "timestamp", System.currentTimeMillis()
        );
        
        String destination = "/user/" + userId + "/notifications";
        messagingTemplate.convertAndSend(destination, completionNotification);
        log.debug("Sent ride completion notification to user {} for ride {}", userId, rideId);
    }

    /**
     * Send driver assignment notification
     */
    public void sendDriverAssignmentNotification(Long userId, Long rideId, String driverName, int etaMinutes) {
        Map<String, Object> assignmentNotification = Map.of(
            "rideId", rideId,
            "driverName", driverName,
            "etaMinutes", etaMinutes,
            "etaFormatted", String.format("%d min", etaMinutes),
            "message", "A driver has been assigned to your ride!",
            "timestamp", System.currentTimeMillis()
        );
        
        String destination = "/user/" + userId + "/notifications";
        messagingTemplate.convertAndSend(destination, assignmentNotification);
        log.debug("Sent driver assignment notification to user {} for ride {}", userId, rideId);
    }
} 