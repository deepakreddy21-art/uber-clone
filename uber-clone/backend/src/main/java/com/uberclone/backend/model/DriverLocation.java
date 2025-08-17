package com.uberclone.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(precision = 10, scale = 8, nullable = false)
    private Double latitude;

    @Column(precision = 11, scale = 8, nullable = false)
    private Double longitude;

    @Column(length = 12, nullable = false)
    private String geohash;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private Double heading; // direction in degrees

    @Column
    private Double speed; // speed in km/h

    @Column
    private Boolean isOnline;

    @Column
    private Boolean isAvailable;

    @Column
    private String vehicleType;

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
}
