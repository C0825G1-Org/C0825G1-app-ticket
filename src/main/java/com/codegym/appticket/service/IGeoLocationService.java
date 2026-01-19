package com.codegym.appticket.service;

import com.codegym.appticket.dto.home.LocationDTO;

public interface IGeoLocationService {
    LocationDTO getLocationFromIP(String ipAddress);
    double[] getCoordinates(String location);
}
