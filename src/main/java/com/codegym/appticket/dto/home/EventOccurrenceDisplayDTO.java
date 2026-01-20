package com.codegym.appticket.dto.home;

import java.time.LocalDateTime;

public interface EventOccurrenceDisplayDTO {
    Long getId();
    String getLocation();
    String getAddressDetail();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
}
