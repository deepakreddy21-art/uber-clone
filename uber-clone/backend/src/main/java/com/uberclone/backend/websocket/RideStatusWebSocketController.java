package com.uberclone.backend.websocket;

import com.uberclone.backend.model.Ride;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class RideStatusWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;

    public RideStatusWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendRideStatusUpdate(Ride ride) {
        messagingTemplate.convertAndSend("/topic/ride-status/" + ride.getId(), ride);
    }
} 