package com.uberclone.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    @Column(nullable = false)
    private String pickupLocation;

    @Column(nullable = false)
    private String dropoffLocation;

    // Coordinates for precise location tracking
    @Column(precision = 10, scale = 8)
    private Double pickupLatitude;

    @Column(precision = 11, scale = 8)
    private Double pickupLongitude;

    @Column(precision = 10, scale = 8)
    private Double dropoffLatitude;

    @Column(precision = 11, scale = 8)
    private Double dropoffLongitude;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column
    private LocalDateTime acceptedAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    // Pricing and distance
    @Column(precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(precision = 10, scale = 2)
    private BigDecimal distanceFare;

    @Column(precision = 10, scale = 2)
    private BigDecimal timeFare;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalFare;

    @Column(precision = 8, scale = 2)
    private BigDecimal distance; // in kilometers

    @Column
    private Integer estimatedDuration; // in minutes

    @Column
    private Integer actualDuration; // in minutes

    // Driver location when accepted
    @Column(precision = 10, scale = 8)
    private Double driverAcceptLatitude;

    @Column(precision = 11, scale = 8)
    private Double driverAcceptLongitude;

    // Cancellation
    @Column
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    private CancelledBy cancelledBy;

    // Rating
    @Column
    private Integer userRating;

    @Column
    private String userReview;

    @Column
    private Integer driverRating;

    @Column
    private String driverReview;

    // Payment
    @Column
    private String paymentMethod;

    @Column
    private String paymentStatus;

    @Column
    private String transactionId;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        REQUESTED,
        SEARCHING_DRIVER,
        DRIVER_ASSIGNED,
        DRIVER_ARRIVING,
        DRIVER_ARRIVED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    public enum CancelledBy {
        USER,
        DRIVER,
        SYSTEM
    }
} 