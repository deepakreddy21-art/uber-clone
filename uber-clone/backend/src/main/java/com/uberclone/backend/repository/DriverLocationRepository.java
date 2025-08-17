package com.uberclone.backend.repository;

import com.uberclone.backend.model.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {
    
    Optional<DriverLocation> findByDriverId(Long driverId);
    
    List<DriverLocation> findByGeohashStartingWithAndIsOnlineTrueAndIsAvailableTrue(String geohashPrefix);
    
    List<DriverLocation> findByGeohashStartingWithAndIsOnlineTrueAndIsAvailableTrueAndVehicleType(
        String geohashPrefix, String vehicleType);
    
    List<DriverLocation> findByIsOnlineTrueAndIsAvailableTrue();
    
    @Query("SELECT dl FROM DriverLocation dl WHERE dl.isOnline = true AND dl.isAvailable = true " +
           "AND FUNCTION('ST_DWithin', FUNCTION('ST_MakePoint', dl.longitude, dl.latitude), " +
           "FUNCTION('ST_MakePoint', :longitude, :latitude), :radiusKm * 1000) = true")
    List<DriverLocation> findDriversWithinRadius(
        @Param("latitude") double latitude, 
        @Param("longitude") double longitude, 
        @Param("radiusKm") double radiusKm);
    
    @Query("SELECT dl FROM DriverLocation dl WHERE dl.geohash LIKE :geohashPrefix% " +
           "AND dl.isOnline = true AND dl.isAvailable = true " +
           "ORDER BY dl.timestamp DESC")
    List<DriverLocation> findRecentDriversByGeohash(@Param("geohashPrefix") String geohashPrefix);
}
