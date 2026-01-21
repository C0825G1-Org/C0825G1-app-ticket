package com.codegym.appticket.service;

import com.codegym.appticket.dto.event.EventCreateDTO;
import com.codegym.appticket.dto.event.EventDTO;
import com.codegym.appticket.dto.event.EventSearchDTO;
import com.codegym.appticket.dto.event.EventUpdateDTO;
import com.codegym.appticket.dto.home.HomeEventDTO;
import com.codegym.appticket.dto.home.NearByEventDTO;
import com.codegym.appticket.dto.home.NearByEventWithOccurrencesDTO;
import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.EventStatus;
import com.codegym.appticket.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IEventService {
        Page<EventDTO> findAll(Pageable pageable);

        Page<HomeEventDTO> findAllEvent(int page, int size);

        Page<EventDTO> search(EventSearchDTO dto, Pageable pageable);

        Page<EventDTO> findByStatus(EventStatus status,
                        Pageable pageable);

        EventDTO findById(Long id);

        EventDTO create(EventCreateDTO dto);

        EventDTO update(Long id, EventUpdateDTO dto);

        void delete(Long id);

        List<TrendingEventDTO> findTopTrendingEvents();

        List<UpComingEventDTO> findUpComingEvents();

        Page<HomeEventDTO> searchHomeEvents(String searchText, Long categoryId, String location, int page, int size,
                        String sort);

        List<NearByEventDTO> findNearbyEvents(Double userLatitude, Double userLongitude, String excludeLocation,
                        int limit);

    List<NearByEventWithOccurrencesDTO> findNearbyEventsGrouped(Double userLatitude, Double userLongitude, String excludeLocation, int limit);

    // User/Organizer methods
    org.springframework.data.domain.Page<Event> findEventsByOrganizer(com.codegym.appticket.entity.User organizer,
            Pageable pageable);

        // Approval Flow
        void approve(Long id);

        void reject(Long id, String reason);

        // Cancel an event
        void cancel(Long eventId, String reason);

        // Restore an event (from Cancelled/Deleted/Rejected -> Pending)
        void restore(Long eventId);

        long countByStatus(EventStatus status);

        long countByStatuses(List<EventStatus> statuses);

        long countAll();
    void incrementViewCount(Long eventId);

    com.codegym.appticket.dto.event.EventStatsDTO getEventStats(Long eventId, Long occurrenceId);
    
    byte[] exportBookedTicketsToExcel(Long eventId, Long occurrenceId) throws java.io.IOException;
}
