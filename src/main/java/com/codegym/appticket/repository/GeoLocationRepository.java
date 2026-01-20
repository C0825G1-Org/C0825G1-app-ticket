package com.codegym.appticket.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class GeoLocationRepository {

    private final RestTemplate restTemplate = new RestTemplate();

    public Double[] getCoordinates(String address) {
        try {
            // Thêm "Vietnam" để API hiểu rõ hơn
            String searchQuery = address + ", Vietnam";
            
            String url = "https://nominatim.openstreetmap.org/search?q=" +
                    URLEncoder.encode(searchQuery, StandardCharsets.UTF_8) +
                    "&format=json&limit=1&addressdetails=1";

            // Thêm User-Agent header (bắt buộc theo chính sách Nominatim)
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "EventTicketApp/1.0 (contact@example.com)");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            log.debug("Calling Nominatim API for: {}", searchQuery);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> results = response.getBody();
            if (results != null && !results.isEmpty()) {
                Map<String, Object> location = results.get(0);
                Double lat = Double.parseDouble(location.get("lat").toString());
                Double lon = Double.parseDouble(location.get("lon").toString());
                
                log.info("Geocoded '{}' -> [{}, {}]", address, lat, lon);
                return new Double[]{lat, lon};
            } else {
                log.warn("No results from Nominatim for: {}", searchQuery);
            }
        } catch (Exception e) {
            log.error("Geocoding error for '{}': {}", address, e.getMessage());
        }
        return null;
    }
}
