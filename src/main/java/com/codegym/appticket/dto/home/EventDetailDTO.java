package com.codegym.appticket.dto.home;

import java.time.LocalDateTime;

public interface EventDetailDTO {
    Long getId();
    String getTitle();
    String getDescription();
    String getLocation();
    String getCategoryName();
    String getMediaUrl();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
}
