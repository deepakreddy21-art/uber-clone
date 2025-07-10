package com.uberclone.backend.service;

import com.uberclone.backend.model.Ride;
import com.uberclone.backend.model.User;
import com.uberclone.backend.repository.RideRepository;
import com.uberclone.backend.repository.UserRepository;
import com.uberclone.backend.websocket.RideStatusWebSocketController;
import com.uberclone.backend.controller.RideController.RideRequest;
import com.uberclone.backend.controller.RideController.EtaRequest;
import com.uberclone.backend.controller.RideController.EtaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RideService {
    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RideStatusWebSocketController rideStatusWebSocketController;
    @Autowired
    private EtaPredictionService etaPredictionService;

    public Ride requestRide(RideRequest request, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        Ride ride = Ride.builder()
                .user(user)
                .pickupLocation(request.getPickupLocation())
                .dropoffLocation(request.getDropoffLocation())
                .requestedAt(LocalDateTime.now())
                .status(Ride.Status.REQUESTED)
                .build();
        rideRepository.save(ride);
        return ride;
    }

    public ResponseEntity<?> acceptRide(Long rideId, Authentication authentication) {
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

    public EtaResponse predictEta(EtaRequest request) {
        double eta = etaPredictionService.predictEta(
            request.getPickupLat(),
            request.getPickupLng(),
            request.getDropoffLat(),
            request.getDropoffLng(),
            request.getHourOfDay()
        );
        return new EtaResponse(eta);
    }
} 