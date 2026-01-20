package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatusAndBookingTimeBefore(BookingStatus status, LocalDateTime threshold);
}
