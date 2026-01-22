package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Ticket t JOIN FETCH t.bookingDetail bd JOIN FETCH bd.booking b WHERE b.user.id = :userId ORDER BY t.id DESC")
    java.util.List<Ticket> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.bookingDetail.booking.id = :bookingId")
    java.util.List<Ticket> findByBookingId(Long bookingId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.bookingDetail.booking.user.id = :userId AND t.bookingDetail.ticketType.eventOccurrence.event.id = :eventId")
    java.util.List<Ticket> findByUserIdAndEventId(Long userId, Long eventId);
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.bookingDetail.booking.user.id = :userId AND t.bookingDetail.ticketType.eventOccurrence.id = :occurrenceId")
    java.util.List<Ticket> findByUserIdAndOccurrenceId(Long userId, Long occurrenceId);

    java.util.Optional<Ticket> findByTicketCode(String ticketCode);

    @org.springframework.data.jpa.repository.Query("SELECT new com.codegym.appticket.dto.ticket.CheckInHistoryDTO(t.ticketCode, t.bookingDetail.booking.user.fullName, t.bookingDetail.ticketType.name, t.bookingDetail.ticketType.eventOccurrence.location.addressDetail, CAST(t.bookingDetail.ticketType.eventOccurrence.startTime AS string), t.checkInTime) FROM Ticket t WHERE t.bookingDetail.ticketType.eventOccurrence.event.id = :eventId AND t.used = true ORDER BY t.checkInTime DESC")
    java.util.List<com.codegym.appticket.dto.ticket.CheckInHistoryDTO> findCheckedInTicketsByEventId(Long eventId);
}
