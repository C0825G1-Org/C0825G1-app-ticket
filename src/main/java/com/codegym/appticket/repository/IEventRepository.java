package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEventRepository extends JpaRepository<Event, Long> {

    // Đếm số sự kiện user đã tạo
    long countByCreatedBy(User createdBy);

    // Lấy danh sách sự kiện user đã tạo, sắp xếp mới nhất
    List<Event> findByCreatedByOrderByCreatedDateDesc(User createdBy, Pageable pageable);
}
