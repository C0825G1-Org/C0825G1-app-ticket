package com.codegym.appticket.dto.report;

import java.math.BigDecimal;

public interface TopOrganizerDTO {
    Long getId();
    String getFullName();
    String getEmail();
    Long getEventCount();
    BigDecimal getTotalRevenue(); // 5% commission
}
