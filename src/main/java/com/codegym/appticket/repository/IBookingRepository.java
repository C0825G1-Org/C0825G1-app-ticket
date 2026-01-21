package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.entity.BookingStatus;
import com.codegym.appticket.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IBookingRepository extends JpaRepository<Booking, Long> {

  // Lấy danh sách booking của user, sắp xếp theo ngày tạo giảm dần
  List<Booking> findByUserOrderByBookingTimeDesc(User user, Pageable pageable);

  // Đếm số booking thành công (nếu cần count số đơn hàng)
  long countByUserAndStatus(User user, BookingStatus status);

  // --- Report Queries ---

  // 1. Count Total Bookings in Period
  @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'SUCCESS' AND b.bookingTime BETWEEN :start AND :end")
  long countSuccessfulBookings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  // 2. Sum Total Revenue (5% of Ticket Sales)
  // Revenue = SUM(quantity * price) * 0.05
  @Query("SELECT SUM(bd.quantity * tt.price) * 0.05 " +
      "FROM Booking b " +
      "JOIN com.codegym.appticket.entity.BookingDetail bd ON bd.booking = b " +
      "JOIN bd.ticketType tt " +
      "WHERE b.status = 'SUCCESS' AND b.bookingTime BETWEEN :start AND :end")
  java.math.BigDecimal sumTotalRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  // 3. Revenue Chart (Group by Day)
  @Query(value = """
      SELECT DATE(b.booking_time) as date, SUM(bd.quantity * tt.price) * 0.05 as revenue
      FROM bookings b
      JOIN booking_details bd ON bd.booking_id = b.id
      JOIN ticket_types tt ON tt.id = bd.ticket_type_id
      WHERE b.status = 'SUCCESS'
        AND b.booking_time BETWEEN :start AND :end
      GROUP BY DATE(b.booking_time)
      ORDER BY DATE(b.booking_time) ASC
      """, nativeQuery = true)
  List<Object[]> getRevenueStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  // 4. Revenue Chart (Group by Month)
  @Query(value = """
      SELECT DATE_FORMAT(b.booking_time, '%Y-%m') as date, SUM(bd.quantity * tt.price) * 0.05 as revenue
      FROM bookings b
      JOIN booking_details bd ON bd.booking_id = b.id
      JOIN ticket_types tt ON tt.id = bd.ticket_type_id
      WHERE b.status = 'SUCCESS'
        AND b.booking_time BETWEEN :start AND :end
      GROUP BY DATE_FORMAT(b.booking_time, '%Y-%m')
      ORDER BY DATE_FORMAT(b.booking_time, '%Y-%m') ASC
      """, nativeQuery = true)
  List<Object[]> getRevenueStatsByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  // 5. Booking Chart (Group by Day) - Count bookings or tickets? Request says
  // 'Bookings Bar Chart'.
  @Query(value = """
      SELECT DATE(booking_time) as date, COUNT(*) as count
      FROM bookings
      WHERE status = 'SUCCESS'
        AND booking_time BETWEEN :start AND :end
      GROUP BY DATE(booking_time)
      ORDER BY DATE(booking_time) ASC
      """, nativeQuery = true)
  List<Object[]> getBookingStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  // 6. Booking Chart (Group by Month)
  @Query(value = """
      SELECT DATE_FORMAT(booking_time, '%Y-%m') as date, COUNT(*) as count
      FROM bookings
      WHERE status = 'SUCCESS'
        AND booking_time BETWEEN :start AND :end
      GROUP BY DATE_FORMAT(booking_time, '%Y-%m')
      ORDER BY DATE_FORMAT(booking_time, '%Y-%m') ASC
      """, nativeQuery = true)
  List<Object[]> getBookingStatsByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  // Find expired pending bookings (for auto-cancellation)
  List<Booking> findByStatusAndBookingTimeBefore(BookingStatus status, LocalDateTime threshold);
}
