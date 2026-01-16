package com.codegym.appticket.dto.home;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface UpComingEventDTO {
    Long getId();
    String getTitle();
    String getDescription();
    String getLocation();
    String getImage();
    String getCategoryName();
    LocalDateTime getStartTime();
    BigDecimal getMinPrice();
}