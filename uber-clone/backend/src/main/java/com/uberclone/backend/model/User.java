package com.uberclone.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Location tracking
    @Column(precision = 10, scale = 8)
    private Double latitude;

    @Column(precision = 11, scale = 8)
    private Double longitude;

    @Column(length = 12)
    private String geohash;

    @Column
    private LocalDateTime lastLocationUpdate;

    // Driver specific fields
    @Column
    private Boolean isAvailable;

    @Column
    private Boolean isOnline;

    @Column
    private String vehicleModel;

    @Column
    private String vehicleColor;

    @Column
    private String licensePlate;

    @Column
    private String vehicleType; // STANDARD, COMFORT, PREMIUM, POOL

    // Driver rating fields
    @Builder.Default
    private Double rating = 0.0;
    
    @Builder.Default
    private Integer ratingCount = 0;

    // Account status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

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

    public enum Role {
        USER,
        DRIVER,
        ADMIN
    }

    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        DEACTIVATED
    }
} 