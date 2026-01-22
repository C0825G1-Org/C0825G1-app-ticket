package com.codegym.appticket.listener;

import com.codegym.appticket.entity.Location;
import com.codegym.appticket.service.IGeoLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA Entity Listener for Location entity
 * Automatically populates latitude and longitude when a location is created or updated
 */
@Slf4j
public class LocationEntityListener {
    
    /**
     * Called before persisting a new Location entity
     * Automatically populates coordinates if missing
     */
    @PrePersist
    public void prePersist(Location location) {
        log.info("[PrePersist] LocationEntityListener triggered for new location");
        populateCoordinates(location, "PrePersist");
    }
    
    /**
     * Called before updating an existing Location entity
     * Automatically populates coordinates if missing
     */
    @PreUpdate
    public void preUpdate(Location location) {
        log.info("[PreUpdate] LocationEntityListener triggered for location ID: {}", location.getId());
        populateCoordinates(location, "PreUpdate");
    }
    
    /**
     * Populate coordinates for a location if they are missing
     */
    private void populateCoordinates(Location location, String event) {
        // Skip if coordinates already exist
        if (location.getLatitude() != null && location.getLongitude() != null) {
            log.debug("[{}] Location already has coordinates, skipping", event);
            return;
        }
        
        try {
            // Get Spring ApplicationContext
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            if (context == null) {
                log.error("[{}] ApplicationContext not available!", event);
                return;
            }
            
            // Get GeoLocationService bean from Spring context
            IGeoLocationService geoLocationService = context.getBean(IGeoLocationService.class);
            
            // Get province name from ward
            if (location.getWard() == null || location.getWard().getProvince() == null) {
                log.warn("[{}] Location has no ward/province information", event);
                return;
            }
            
            String provinceName = location.getWard().getProvince().getName();
            log.info("[{}] Getting coordinates for province: {}", event, provinceName);
            
            // Get coordinates for the province
            Double[] coords = geoLocationService.getCoordinates(provinceName);
            
            if (coords != null && coords.length == 2) {
                location.setLatitude(coords[0]);
                location.setLongitude(coords[1]);
                log.info("[{}] ✅ Auto-populated coordinates for province '{}': lat={}, lon={}", 
                        event, provinceName, coords[0], coords[1]);
            } else {
                log.warn("[{}] ❌ Could not find coordinates for province: {}", event, provinceName);
            }
        } catch (Exception e) {
            log.error("[{}] ❌ Error auto-populating coordinates: {}", event, e.getMessage(), e);
        }
    }
}
