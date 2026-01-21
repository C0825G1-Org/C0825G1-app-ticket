package com.codegym.appticket.util;

import com.codegym.appticket.entity.Location;
import com.codegym.appticket.repository.ILocationRepository;
import com.codegym.appticket.service.IGeoLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility to populate latitude and longitude for existing locations
 * 
 * Note: New locations are automatically populated via LocationEntityListener
 * This utility is only for manually updating existing locations if needed
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LocationCoordinatePopulator {
    
    private final ILocationRepository locationRepository;
    private final IGeoLocationService geoLocationService;

    /**
     * Manual trigger method - can be called from services if needed
     * @return number of locations updated
     */
    public int populateLocationCoordinates() {
        log.info("Starting to populate location coordinates...");
        
        List<Location> locations = locationRepository.findAll();
        int updated = 0;
        int failed = 0;
        int skipped = 0;
        
        for (Location location : locations) {
            // Skip if already has coordinates
            if (location.getLatitude() != null && location.getLongitude() != null) {
                skipped++;
                continue;
            }
            
            try {
                // Get province name from ward
                String provinceName = location.getWard().getProvince().getName();
                
                // Get coordinates for the province
                Double[] coords = geoLocationService.getCoordinates(provinceName);
                
                if (coords != null && coords.length == 2) {
                    location.setLatitude(coords[0]);
                    location.setLongitude(coords[1]);
                    locationRepository.save(location);
                    updated++;
                    log.info("Updated location ID {} with coordinates for {}", 
                            location.getId(), provinceName);
                } else {
                    failed++;
                    log.warn("Could not find coordinates for location ID {} ({})", 
                            location.getId(), provinceName);
                }
            } catch (Exception e) {
                failed++;
                log.error("Error updating location ID {}: {}", location.getId(), e.getMessage());
            }
        }
        
        log.info("Location coordinate population completed. Updated: {}, Skipped: {}, Failed: {}", 
                updated, skipped, failed);
        return updated;
    }
}
