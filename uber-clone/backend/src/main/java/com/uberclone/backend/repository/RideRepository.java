package com.uberclone.backend.repository;

import com.uberclone.backend.model.Ride;
import com.uberclone.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByUser(User user);
    List<Ride> findByDriver(User driver);
    List<Ride> findByStatus(Ride.Status status);
} 