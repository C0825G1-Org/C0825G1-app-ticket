package com.codegym.appticket.repository;

import com.codegym.appticket.entity.EventCancellationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEventCancellationHistoryRepository extends JpaRepository<EventCancellationHistory, Long> {
    List<EventCancellationHistory> findByEventIdOrderByCreatedDateDesc(Long eventId);
}
