package com.codegym.appticket.dto.home;

import lombok.Data;

@Data
public class IpApiResponse {
    private String status;
    private Double lat;
    private Double lon;
    private String city;
    private String country;
}
