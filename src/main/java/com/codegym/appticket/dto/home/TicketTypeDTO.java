package com.codegym.appticket.dto.home;

public interface TicketTypeDTO {
    Long getId();
    String getName();
    Double getPrice();
    Integer getAvailableQuantity();
}
