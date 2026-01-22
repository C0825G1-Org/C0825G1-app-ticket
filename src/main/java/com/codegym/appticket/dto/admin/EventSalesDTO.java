package com.codegym.appticket.dto.admin;

import java.time.LocalDateTime;

public interface EventSalesDTO {
    Long getId();

    String getTitle();

    LocalDateTime getStartTime();

    Long getSoldTickets();

    Long getTotalTickets();

    // Calculated field can be done in service or view, but interface projection
    // usually needs exact sql columns
    // We will do calculation in thymeleaf or service
}
