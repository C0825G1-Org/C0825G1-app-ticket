package com.codegym.appticket.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface RecentBookingDTO {
    Long getId();

    String getUserFullName();

    String getEventTitle();

    String getTransactionCode();

    String getTicketTypes();

    LocalDateTime getBookingTime();

    BigDecimal getTotalPrice();
}
