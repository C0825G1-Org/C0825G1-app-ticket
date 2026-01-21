package com.codegym.appticket.repository;

import com.codegym.appticket.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT bd FROM BookingDetail bd " +
            "JOIN FETCH bd.ticketType tt " +
            "JOIN FETCH tt.eventOccurrence eo " +
            "JOIN FETCH eo.event " +
            "WHERE bd.booking.id = :bookingId")
    java.util.List<BookingDetail> findByBookingIdWithAssociations(Long bookingId);

    java.util.List<BookingDetail> findByBookingId(Long bookingId);
}
