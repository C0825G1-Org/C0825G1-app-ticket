package com.codegym.appticket.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatsDTO {
    private Long totalTicketsSold;
    private BigDecimal totalRevenue;
    private Long viewCount;
    private List<BookedTicketDTO> bookedTickets;
}
