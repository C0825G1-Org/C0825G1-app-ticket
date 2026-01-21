package com.codegym.appticket.repository;

import com.codegym.appticket.entity.BookingDetail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface IBookingDetailRepository extends CrudRepository<BookingDetail, Long> {

      // Tính tổng số lượng vé user đã mua (card user detail)
      @Query("SELECT COALESCE(SUM(bd.quantity), 0) FROM BookingDetail bd " +
                  "WHERE bd.booking.user.id = :userId AND bd.booking.status = 'SUCCESS'")
      Long countTicketsByUserId(Long userId);

      @Query("SELECT COALESCE(SUM(bd.quantity * bd.ticketType.price), 0) FROM BookingDetail bd " +
                  "WHERE bd.booking.user.id = :userId AND bd.booking.status = 'SUCCESS'")
      BigDecimal sumTotalSpentByUserId(Long userId);

      // Tính tổng doanh thu toàn hệ thống (cho admin dashboard)
      @Query("SELECT COALESCE(SUM(bd.quantity * bd.ticketType.price), 0) FROM BookingDetail bd " +
                  "WHERE bd.booking.status = 'SUCCESS'")
      BigDecimal sumTotalRevenue();

      // Tính tổng vé đã bán toàn hệ thống (cho admin dashboard)
      @Query("SELECT COALESCE(SUM(bd.quantity), 0) FROM BookingDetail bd " +
                  "WHERE bd.booking.status = 'SUCCESS'")
      Long countTotalTicketsSold();

      List<BookingDetail> findByBooking(com.codegym.appticket.entity.Booking booking);

      // Find booking details by booking ID
      List<BookingDetail> findByBookingId(Long bookingId);
}
