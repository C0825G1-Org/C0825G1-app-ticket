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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IEventService {
    Page<EventDTO> findAll(Pageable pageable);

    Page<HomeEventDTO> findAllEvent(int page, int size);

    Page<EventDTO> search(EventSearchDTO dto, Pageable pageable);

    org.springframework.data.domain.Page<EventDTO> findByStatus(com.codegym.appticket.entity.EventStatus status,
            org.springframework.data.domain.Pageable pageable);

    EventDTO findById(Long id);

    EventDTO create(EventCreateDTO dto);

    EventDTO update(Long id, EventUpdateDTO dto);

    void delete(Long id);

    List<TrendingEventDTO> findTopTrendingEvents();

    List<UpComingEventDTO> findUpComingEvents();

    Page<HomeEventDTO> searchHomeEvents(String searchText, Long categoryId, String location, int page, int size,
            String sort);

    List<NearByEventDTO> findNearbyEvents(Double userLatitude, Double userLongitude, String excludeLocation, int limit);

    List<NearByEventWithOccurrencesDTO> findNearbyEventsGrouped(Double userLatitude, Double userLongitude, String excludeLocation, int limit);

    // User/Organizer methods
    org.springframework.data.domain.Page<Event> findEventsByOrganizer(com.codegym.appticket.entity.User organizer,
            Pageable pageable);

    // Approval Flow
    void approve(Long id);

    void reject(Long id, String reason);

    long countByStatus(EventStatus status);

    long countAll();
}
