package com.uberclone.backend.controller;

import com.uberclone.backend.model.User;
import com.uberclone.backend.repository.UserRepository;
import com.uberclone.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestParam String message, @RequestParam Long userId) {
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        User user = userOpt.get();
        notificationService.sendEmail(user.getEmail(), "Notification", message);
        return ResponseEntity.ok("Notification sent to " + user.getEmail());
    }
} 