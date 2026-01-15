package com.codegym.appticket.dto.home;

import java.time.LocalDateTime;

public interface UpComingEventDTO {
    Long getId();
    String getTitle();
    String getLocation();
    LocalDateTime getStartTime();
}