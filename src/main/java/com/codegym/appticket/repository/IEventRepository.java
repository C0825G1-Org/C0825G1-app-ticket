package com.codegym.appticket.repository;

import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.entity.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IEventRepository extends JpaRepository<Event, Long> {

    @Query(value = """
            SELECT 
                e.id AS id,
                e.title AS title,
                SUM(bd.quantity) AS totalTickets
            FROM events e
            JOIN ticket_types tt ON tt.event_id = e.id
            JOIN booking_details bd ON bd.ticket_type_id = tt.id
            JOIN bookings b ON b.id = bd.booking_id
            WHERE b.status = 'SUCCESS'
              AND e.status = 'APPROVED'
            GROUP BY e.id
            ORDER BY totalTickets DESC
            LIMIT 3
        """, nativeQuery = true)
    List<TrendingEventDTO> findTopTrendingEvents();

    @Query("""
            SELECT e.id AS id,
                   e.title AS title,
                   e.location AS location,
                   MIN(et.startTime) AS startTime
            FROM Event e
            JOIN e.eventTimes et
            WHERE et.startTime > CURRENT_TIMESTAMP
              AND e.status = 'APPROVED'
            GROUP BY e.id, e.title, e.location
            ORDER BY startTime ASC
        """)
    List<UpComingEventDTO> findTopUpcomingEvents(Pageable pageable);

    @Query("""
            SELECT e.id AS id,
                   e.title AS title,
                   e.location AS location,
                   MIN(et.startTime) AS startTime
            FROM Event e
            JOIN e.eventTimes et
            WHERE et.startTime > CURRENT_TIMESTAMP
              AND e.status = 'APPROVED'
            GROUP BY e.id, e.title, e.location
            ORDER BY startTime ASC
        """)
    List<UpComingEventDTO> findUpComingEvents();
}
