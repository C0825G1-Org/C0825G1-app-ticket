package com.codegym.appticket.dto.report;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportSummaryDTO {
    private BigDecimal totalRevenue;
    private Double revenueChange; // percentage

    private Long totalBookings;
    private Double bookingsChange;

    private Long totalEvents;
    private Double eventsChange;

    private Long totalUsers;
    private Double usersChange;
}
