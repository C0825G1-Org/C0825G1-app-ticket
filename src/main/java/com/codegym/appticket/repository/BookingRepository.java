package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT b FROM Booking b JOIN FETCH b.user WHERE b.id = :id")
    java.util.Optional<Booking> findByIdWithUser(Long id);

    List<Booking> findByStatusAndBookingTimeBefore(BookingStatus status, LocalDateTime threshold);
}
