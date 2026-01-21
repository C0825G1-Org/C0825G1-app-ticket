package com.codegym.appticket.dto.ticket;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketCheckInResponse {
    public enum Status {
        SUCCESS,
        INVALID_EVENT,
        ALREADY_CHECKED_IN,
        INVALID_TIME,
        NOT_FOUND,
        ERROR
    }

    private Status status;
    private String message;
    private String ticketCode;
    private String customerName;
    private String ticketType;
    private LocalDateTime checkInTime;
}
