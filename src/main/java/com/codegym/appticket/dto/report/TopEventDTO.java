package com.codegym.appticket.dto.report;

import java.math.BigDecimal;

public interface TopEventDTO {
    Long getId();
    String getTitle();
    String getCategoryName();
    Long getTicketsSold();
    BigDecimal getRevenue(); // This should be the 5% revenue
}
