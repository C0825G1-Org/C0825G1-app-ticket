package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import com.codegym.appticket.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface IEventRepository extends JpaRepository<Event, Long> {

        @Query("SELECT DISTINCT e FROM Event e " +
                        "LEFT JOIN e.eventTimes t " +
                        "WHERE (:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
                        "AND (:categoryId IS NULL OR e.category.id = :categoryId) " +
                        "AND (:startDateTime IS NULL OR t.startTime >= :startDateTime) " +
                        "AND (:endDateTime IS NULL OR t.endTime <= :endDateTime)")
        org.springframework.data.domain.Page<Event> searchEvents(@Param("title") String title,
                        @Param("categoryId") Long categoryId,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime,
                        org.springframework.data.domain.Pageable pageable);
    // Đếm số sự kiện user đã tạo
    long countByCreatedBy(User createdBy);

    // Lấy danh sách sự kiện user đã tạo, sắp xếp mới nhất
    List<Event> findByCreatedByOrderByCreatedDateDesc(User createdBy, Pageable pageable);
}
