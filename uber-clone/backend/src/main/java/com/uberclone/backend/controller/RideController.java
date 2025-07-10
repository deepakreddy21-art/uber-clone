package com.uberclone.backend.controller;

import com.uberclone.backend.model.Ride;
import com.uberclone.backend.model.User;
import com.uberclone.backend.repository.RideRepository;
import com.uberclone.backend.repository.UserRepository;
import com.uberclone.backend.websocket.RideStatusWebSocketController;
import com.uberclone.backend.service.EtaPredictionService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rides")
public class RideController {
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RideStatusWebSocketController rideStatusWebSocketController;
    @Autowired
    private EtaPredictionService etaPredictionService;

    @PostMapping("/request")
    public ResponseEntity<?> requestRide(@RequestBody RideRequest request, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        Ride ride = Ride.builder()
                .user(user)
                .pickupLocation(request.getPickupLocation())
                .dropoffLocation(request.getDropoffLocation())
                .requestedAt(LocalDateTime.now())
                .status(Ride.Status.REQUESTED)
                .build();
        rideRepository.save(ride);
        return ResponseEntity.ok(ride);
    }

    @GetMapping("/available")
    public List<Ride> getAvailableRides() {
        return rideRepository.findByStatus(Ride.Status.REQUESTED);
    }

    @PostMapping("/accept/{rideId}")
    public ResponseEntity<?> acceptRide(@PathVariable Long rideId, Authentication authentication) {
        User driver = userRepository.findByEmail(authentication.getName()).orElseThrow();
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (rideOpt.isEmpty() || rideOpt.get().getStatus() != Ride.Status.REQUESTED) {
            return ResponseEntity.badRequest().body("Ride not available");
        }
        Ride ride = rideOpt.get();
        ride.setDriver(driver);
        ride.setStatus(Ride.Status.ACCEPTED);
        rideRepository.save(ride);
        rideStatusWebSocketController.sendRideStatusUpdate(ride);
        return ResponseEntity.ok(ride);
    }

    @GetMapping("/my")
    public List<Ride> getMyRides(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        if (user.getRole() == User.Role.DRIVER) {
            return rideRepository.findByDriver(user);
        } else {
            return rideRepository.findByUser(user);
        }
    }

    @PostMapping("/predict-eta")
    public ResponseEntity<?> predictEta(@RequestBody EtaRequest request) {
        double eta = etaPredictionService.predictEta(
            request.getPickupLat(),
            request.getPickupLng(),
            request.getDropoffLat(),
            request.getDropoffLng(),
            request.getHourOfDay()
        );
        return ResponseEntity.ok(new EtaResponse(eta));
    }

    @Data
    public static class RideRequest {
        private String pickupLocation;
        private String dropoffLocation;
    }

    @Data
    public static class EtaRequest {
        private double pickupLat;
        private double pickupLng;
        private double dropoffLat;
        private double dropoffLng;
        private int hourOfDay;
    }

    @Data
    public static class EtaResponse {
        private final double etaMinutes;
    }
} 