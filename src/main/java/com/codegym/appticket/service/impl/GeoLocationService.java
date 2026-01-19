package com.codegym.appticket.service.impl;

import com.codegym.appticket.config.WebClientConfig;
import com.codegym.appticket.dto.home.IpApiResponse;
import com.codegym.appticket.dto.home.LocationDTO;
import com.codegym.appticket.repository.GeoLocationRepository;
import com.codegym.appticket.service.IGeoLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class GeoLocationService implements IGeoLocationService {
    private final WebClient ipApiWebClient;
    private final GeoLocationRepository geoLocationRepository;

    public GeoLocationService(WebClient ipApiWebClient, GeoLocationRepository geoLocationRepository) {
        this.ipApiWebClient = ipApiWebClient;
        this.geoLocationRepository = geoLocationRepository;
    }

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
    public double[] getCoordinates(String location) {
        return geoLocationRepository.getCoordinates(location);
    }
}
