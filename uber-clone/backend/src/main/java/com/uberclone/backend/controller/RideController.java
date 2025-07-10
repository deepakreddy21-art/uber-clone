package com.uberclone.backend.controller;

import com.uberclone.backend.model.Ride;
import com.uberclone.backend.model.User;
import com.uberclone.backend.repository.RideRepository;
import com.uberclone.backend.repository.UserRepository;
import com.uberclone.backend.service.RideService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@Validated
public class RideController {
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RideService rideService;

    /**
     * Request a new ride.
     * @param request Ride request body
     * @param authentication Authenticated user
     * @return The created ride
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestRide(@Valid @RequestBody RideRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(rideService.requestRide(request, authentication));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to request ride: " + e.getMessage());
        }
    }

    @GetMapping("/available")
    public List<Ride> getAvailableRides() {
        return rideRepository.findByStatus(Ride.Status.REQUESTED);
    }

    /**
     * Accept a ride as a driver.
     * @param rideId Ride ID
     * @param authentication Authenticated driver
     * @return The accepted ride
     */
    @PostMapping("/accept/{rideId}")
    public ResponseEntity<?> acceptRide(@PathVariable Long rideId, Authentication authentication) {
        try {
            return rideService.acceptRide(rideId, authentication);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to accept ride: " + e.getMessage());
        }
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

    /**
     * Predict ETA for a ride.
     * @param request ETA request body
     * @return ETA in minutes
     */
    @PostMapping("/predict-eta")
    public ResponseEntity<?> predictEta(@Valid @RequestBody EtaRequest request) {
        try {
            return ResponseEntity.ok(rideService.predictEta(request));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to predict ETA: " + e.getMessage());
        }
    }

    @Data
    public static class RideRequest {
        @NotBlank
        private String pickupLocation;
        @NotBlank
        private String dropoffLocation;
    }

    @Data
    public static class EtaRequest {
        @NotNull
        private Double pickupLat;
        @NotNull
        private Double pickupLng;
        @NotNull
        private Double dropoffLat;
        @NotNull
        private Double dropoffLng;
        @NotNull
        private Integer hourOfDay;
    }

    @Data
    public static class EtaResponse {
        private final double etaMinutes;
    }
} 