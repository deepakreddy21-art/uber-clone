package com.uberclone.backend.controller;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.uberclone.backend.model.PaymentTransaction;
import com.uberclone.backend.model.User;
import com.uberclone.backend.repository.PaymentTransactionRepository;
import com.uberclone.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentRequest request) throws Exception {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount())
                .setCurrency("usd")
                .build();
        PaymentIntent intent = PaymentIntent.create(params);
        Map<String, String> responseData = new HashMap<>();
        responseData.put("clientSecret", intent.getClientSecret());
        // Save transaction
        User user = userRepository.findById(request.getUserId()).orElse(null);
        PaymentTransaction txn = PaymentTransaction.builder()
                .paymentId(intent.getId())
                .user(user)
                .amount(request.getAmount())
                .status(intent.getStatus())
                .createdAt(LocalDateTime.now())
                .build();
        paymentTransactionRepository.save(txn);
        return ResponseEntity.ok(responseData);
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentId) throws Exception {
        PaymentIntent intent = PaymentIntent.retrieve(paymentId);
        return ResponseEntity.ok(Map.of(
            "id", intent.getId(),
            "status", intent.getStatus(),
            "amount", intent.getAmount(),
            "currency", intent.getCurrency()
        ));
    }

    @PostMapping("/refund/{paymentId}")
    public ResponseEntity<?> refundPayment(@PathVariable String paymentId) throws Exception {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentId)
                .build();
        Refund refund = Refund.create(params);
        // Update transaction status
        PaymentTransaction txn = paymentTransactionRepository.findAll().stream()
            .filter(t -> t.getPaymentId().equals(paymentId)).findFirst().orElse(null);
        if (txn != null) {
            txn.setStatus("refunded");
            paymentTransactionRepository.save(txn);
        }
        return ResponseEntity.ok(Map.of(
            "id", refund.getId(),
            "status", refund.getStatus()
        ));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getTransactionHistory(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");
        var txns = paymentTransactionRepository.findByUser(user);
        return ResponseEntity.ok(txns);
    }

    @Data
    public static class PaymentRequest {
        private Long rideId;
        private Long userId;
        private Long amount; // in cents
    }
} 