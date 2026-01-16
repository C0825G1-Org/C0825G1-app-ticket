package com.codegym.appticket.dto.home;

import java.time.LocalDateTime;

public interface HomeEventDTO {
    Long getId();
    String getTitle();
    String getDescription();
    String getLocation();
    String getMediaUrl();
    String getCategoryName();
    LocalDateTime getStartTime();
    Double getPrice();
}