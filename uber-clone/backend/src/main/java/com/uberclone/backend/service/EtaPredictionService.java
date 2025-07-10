package com.uberclone.backend.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EtaPredictionService {
    @Value("${ml.eta.url:http://localhost:8001/predict-eta}")
    private String mlEtaUrl;

    public double predictEta(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng, int hourOfDay) {
        RestTemplate restTemplate = new RestTemplate();
        EtaRequest req = new EtaRequest(pickupLat, pickupLng, dropoffLat, dropoffLng, hourOfDay);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EtaRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<EtaResponse> response = restTemplate.postForEntity(mlEtaUrl, entity, EtaResponse.class);
        EtaResponse body = response.getBody();
        if (body == null) {
            return -1;
        }
        return body.getEta_minutes();
    }

    @Data
    public static class EtaRequest {
        private final double pickup_lat;
        private final double pickup_lng;
        private final double dropoff_lat;
        private final double dropoff_lng;
        private final int hour_of_day;
    }

    @Data
    public static class EtaResponse {
        private double eta_minutes;
    }
} 