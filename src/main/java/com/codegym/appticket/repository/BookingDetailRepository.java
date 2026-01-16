package com.codegym.appticket.repository;

import com.codegym.appticket.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    java.util.List<BookingDetail> findByBookingId(Long bookingId);
}
