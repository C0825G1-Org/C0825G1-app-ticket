package com.codegym.appticket.dto.ticket;

import lombok.Data;

@Data
public class TicketCheckInRequest {
    private String ticketCode;
    private Long eventId;
    private Double latitude;
    private Double longitude;
}
