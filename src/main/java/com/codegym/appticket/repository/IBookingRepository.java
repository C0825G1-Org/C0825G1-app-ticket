package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.entity.BookingStatus;
import com.codegym.appticket.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBookingRepository extends JpaRepository<Booking, Long> {
    
    // Lấy danh sách booking của user, sắp xếp theo ngày tạo giảm dần
    List<Booking> findByUserOrderByBookingTimeDesc(User user, Pageable pageable);
    
    // Đếm số booking thành công (nếu cần count số đơn hàng)
    long countByUserAndStatus(User user, BookingStatus status);
}
