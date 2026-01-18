package com.codegym.appticket.repository;

import com.codegym.appticket.entity.EventMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEventMediaRepository extends JpaRepository<EventMedia, Long> {
    void deleteByEventId(Long eventId);

    List<EventMedia> findByEventId(Long eventId);
}
