package com.codegym.appticket.repository;

import com.codegym.appticket.entity.EventTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IEventTimeRepository extends JpaRepository<EventTime, Long> {
    void deleteByEventId(Long eventId);
}
