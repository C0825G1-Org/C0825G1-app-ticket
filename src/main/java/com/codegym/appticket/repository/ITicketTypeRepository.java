package com.codegym.appticket.repository;

import com.codegym.appticket.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITicketTypeRepository extends JpaRepository<TicketType, Long> {
    List<TicketType> findByEventOccurrence_Event_Id(Long eventId);

    void deleteByEventOccurrence_Event_Id(Long eventId);
}
