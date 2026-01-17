package com.codegym.appticket.dto.home;

import java.time.LocalDateTime;

public interface TrendingEventDTO {
    Long getId();
    String getTitle();
    String getDescription();
    String getLocation();
    String getImage();
    LocalDateTime getEventDate();
    String getCategoryName();
    Long getTotalTickets();
}
