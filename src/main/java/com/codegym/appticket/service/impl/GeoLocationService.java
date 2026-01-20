package com.codegym.appticket.service.impl;

import com.codegym.appticket.config.WebClientConfig;
import com.codegym.appticket.dto.home.IpApiResponse;
import com.codegym.appticket.dto.home.LocationDTO;
import com.codegym.appticket.repository.GeoLocationRepository;
import com.codegym.appticket.service.IGeoLocationService;
import com.codegym.appticket.util.VietnamProvinceCoordinates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeoLocationService implements IGeoLocationService {
    private final WebClient ipApiWebClient;
    private final GeoLocationRepository geoLocationRepository;
    private final VietnamProvinceCoordinates vietnamProvinceCoordinates;

    @Override
    public LocationDTO getLocationFromIP(String ipAddress) {
        try {
            IpApiResponse response = ipApiWebClient.get()
                    .uri("/json/{ip}", ipAddress)
                    .retrieve()
                    .bodyToMono(IpApiResponse.class)
                    .block();

            if (response != null && "success".equals(response.getStatus())) {
                return new LocationDTO(response.getLat(), response.getLon());
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy vị trí từ IP: {}", ipAddress, e);
        }

        // fallback: Hà Nội
        return new LocationDTO(21.028511, 105.804817);
    }

    @Override
    public Double[] getCoordinates(String location) {
        // Dùng map tọa độ Việt Nam (đơn giản, nhanh)
        Double[] coords = vietnamProvinceCoordinates.getCoordinates(location);
        
        if (coords != null) {
            log.info("Found coordinates for '{}': [{}, {}]", location, coords[0], coords[1]);
            return coords;
        }
        
        log.warn("Province '{}' not found in map", location);
        return null;
    }
}
