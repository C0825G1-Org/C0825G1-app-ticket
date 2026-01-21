package com.codegym.appticket.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookedTicketDTO {
    private String ticketCode;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String ticketTypeName;
    private Integer quantity;
    private BigDecimal totalPrice;
    private LocalDateTime bookingTime;
    private String status; // SUCCESS, PENDING...
    private String occurrenceTime; // Display string for occurrence
}
