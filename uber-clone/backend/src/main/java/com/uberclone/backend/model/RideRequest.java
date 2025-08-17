package com.uberclone.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ride_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String pickupLocation;

    @Column(nullable = false)
    private String dropoffLocation;

    @Column(precision = 10, scale = 8, nullable = false)
    private Double pickupLatitude;

    @Column(precision = 11, scale = 8, nullable = false)
    private Double pickupLongitude;

    @Column(precision = 10, scale = 8, nullable = false)
    private Double dropoffLatitude;

    @Column(precision = 11, scale = 8, nullable = false)
    private Double dropoffLongitude;

    @Column(length = 12, nullable = false)
    private String pickupGeohash;

    @Column(length = 12, nullable = false)
    private String dropoffGeohash;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private String vehicleType;

    @Column
    private Integer estimatedDuration;

    @Column(precision = 8, scale = 2)
    private BigDecimal estimatedDistance;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedFare;

    @Column
    private String specialInstructions;

    @Column
    private Integer passengerCount;

    @Column
    private String paymentMethod;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMinutes(5); // 5 minutes expiry
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING,
        SEARCHING_DRIVER,
        DRIVER_FOUND,
        ACCEPTED,
        EXPIRED,
        CANCELLED
    }
}
