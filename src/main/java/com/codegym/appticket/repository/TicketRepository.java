package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.bookingDetail.booking.user.id = :userId ORDER BY t.id DESC")
    java.util.List<Ticket> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.bookingDetail.booking.id = :bookingId")
    java.util.List<Ticket> findByBookingId(Long bookingId);
}
