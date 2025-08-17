package com.uberclone.backend.repository;

import com.uberclone.backend.model.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {
    
    List<RideRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<RideRequest> findByStatus(RideRequest.Status status);
    
    List<RideRequest> findByStatusAndCreatedAtBefore(RideRequest.Status status, LocalDateTime before);
    
    @Query("SELECT rr FROM RideRequest rr WHERE rr.status = 'PENDING' AND rr.expiresAt < :now")
    List<RideRequest> findExpiredRequests(@Param("now") LocalDateTime now);
    
    @Query("SELECT rr FROM RideRequest rr WHERE rr.pickupGeohash LIKE :geohashPrefix% " +
           "AND rr.status = 'PENDING' ORDER BY rr.createdAt ASC")
    List<RideRequest> findPendingRequestsByGeohash(@Param("geohashPrefix") String geohashPrefix);
    
    Optional<RideRequest> findByUserIdAndStatusIn(Long userId, List<RideRequest.Status> statuses);
    
    @Query("SELECT COUNT(rr) FROM RideRequest rr WHERE rr.status = 'PENDING' " +
           "AND rr.pickupGeohash LIKE :geohashPrefix%")
    long countPendingRequestsByGeohash(@Param("geohashPrefix") String geohashPrefix);
}
