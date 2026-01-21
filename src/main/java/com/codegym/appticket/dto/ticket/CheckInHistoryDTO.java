package com.codegym.appticket.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CheckInHistoryDTO {
    private String ticketCode;
    private String customerName;
    private String ticketType; // e.g., VIP, Standard
    private String eventAddress;
    private String eventTime; // Start time string
    private LocalDateTime checkInTime;
}
