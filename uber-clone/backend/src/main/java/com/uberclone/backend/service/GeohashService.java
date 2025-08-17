package com.uberclone.backend.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GeohashService {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static final int PRECISION = 12;

    /**
     * Encode latitude and longitude to geohash
     */
    public String encode(double latitude, double longitude) {
        return encode(latitude, longitude, PRECISION);
    }

    /**
     * Encode latitude and longitude to geohash with specified precision
     */
    public String encode(double latitude, double longitude, int precision) {
        if (precision < 1 || precision > 12) {
            throw new IllegalArgumentException("Precision must be between 1 and 12");
        }

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        boolean isEven = true;
        int bit = 0;
        int ch = 0;
        StringBuilder geohash = new StringBuilder();

        while (geohash.length() < precision) {
            if (isEven) {
                double mid = (lonRange[0] + lonRange[1]) / 2.0;
                if (longitude >= mid) {
                    ch |= (1 << (4 - bit));
                    lonRange[0] = mid;
                } else {
                    lonRange[1] = mid;
                }
            } else {
                double mid = (latRange[0] + latRange[1]) / 2.0;
                if (latitude >= mid) {
                    ch |= (1 << (4 - bit));
                    latRange[0] = mid;
                } else {
                    latRange[1] = mid;
                }
            }

            isEven = !isEven;
            bit++;

            if (bit == 5) {
                geohash.append(BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }

        return geohash.toString();
    }

    /**
     * Decode geohash to latitude and longitude
     */
    public double[] decode(String geohash) {
        if (geohash == null || geohash.isEmpty()) {
            throw new IllegalArgumentException("Geohash cannot be null or empty");
        }

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        boolean isEven = true;

        for (char c : geohash.toCharArray()) {
            int cd = BASE32.indexOf(c);
            if (cd == -1) {
                throw new IllegalArgumentException("Invalid character in geohash: " + c);
            }

            for (int j = 4; j >= 0; j--) {
                int mask = 1 << j;
                if (isEven) {
                    if ((cd & mask) != 0) {
                        lonRange[0] = (lonRange[0] + lonRange[1]) / 2.0;
                    } else {
                        lonRange[1] = (lonRange[0] + lonRange[1]) / 2.0;
                    }
                } else {
                    if ((cd & mask) != 0) {
                        latRange[0] = (latRange[0] + latRange[1]) / 2.0;
                    } else {
                        latRange[1] = (latRange[0] + latRange[1]) / 2.0;
                    }
                }
                isEven = !isEven;
            }
        }

        double latitude = (latRange[0] + latRange[1]) / 2.0;
        double longitude = (lonRange[0] + lonRange[1]) / 2.0;

        return new double[]{latitude, longitude};
    }

    /**
     * Get neighboring geohashes
     */
    public List<String> getNeighbors(String geohash) {
        double[] coords = decode(geohash);
        double lat = coords[0];
        double lon = coords[1];

        List<String> neighbors = new ArrayList<>();
        
        // Calculate precision to determine step size
        double latStep = 180.0 / Math.pow(2, (geohash.length() * 5) / 2);
        double lonStep = 360.0 / Math.pow(2, (geohash.length() * 5) / 2);

        // Add all 8 neighbors
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                
                double newLat = lat + (i * latStep);
                double newLon = lon + (j * lonStep);
                
                // Handle wrapping around the globe
                if (newLon > 180) newLon -= 360;
                if (newLon < -180) newLon += 360;
                
                if (newLat >= -90 && newLat <= 90) {
                    neighbors.add(encode(newLat, newLon, geohash.length()));
                }
            }
        }

        return neighbors;
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * Get geohash prefixes for a given radius (in km)
     */
    public List<String> getGeohashPrefixes(double latitude, double longitude, double radiusKm) {
        List<String> prefixes = new ArrayList<>();
        
        // Start with high precision and reduce until we cover the radius
        for (int precision = 12; precision >= 1; precision--) {
            String geohash = encode(latitude, longitude, precision);
            // double[] coords = decode(geohash); // Removed unused variable
            
            // Calculate the size of this geohash cell
            double cellSize = calculateCellSize(precision);
            
            if (cellSize <= radiusKm * 2) {
                prefixes.add(geohash);
                break;
            }
        }
        
        return prefixes;
    }

    /**
     * Calculate the approximate size of a geohash cell
     */
    private double calculateCellSize(int precision) {
        // Approximate cell size in kilometers
        double latSize = 180.0 / Math.pow(2, (precision * 5) / 2);
        double lonSize = 360.0 / Math.pow(2, (precision * 5) / 2);
        
        // Convert to kilometers (approximate)
        return Math.max(latSize * 111.0, lonSize * 111.0 * Math.cos(Math.PI / 4));
    }

    /**
     * Check if two geohashes are neighbors
     */
    public boolean areNeighbors(String geohash1, String geohash2) {
        if (geohash1.length() != geohash2.length()) {
            return false;
        }
        
        List<String> neighbors = getNeighbors(geohash1);
        return neighbors.contains(geohash2);
    }
}
