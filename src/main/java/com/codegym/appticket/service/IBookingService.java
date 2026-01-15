package com.codegym.appticket.service;

import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.TicketType;
import java.util.List;
import java.util.Map;

public interface IBookingService {
    Event getEventById(Long eventId);
    List<TicketType> getTicketTypesByEventId(Long eventId);
    Booking createBooking(Long eventId, Long userId, Map<Long, Integer> ticketQuantities);
    Booking getBookingById(Long id);
    void cancelBooking(Long bookingId);
    com.codegym.appticket.entity.User getUserByEmail(String email);
    java.util.List<com.codegym.appticket.entity.Ticket> getTicketsByBookingId(Long bookingId);
}
