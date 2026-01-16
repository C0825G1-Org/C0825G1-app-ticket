package com.codegym.appticket.service;

import com.codegym.appticket.dto.event.EventCreateDTO;
import com.codegym.appticket.dto.event.EventDTO;
import com.codegym.appticket.dto.event.EventSearchDTO;
import com.codegym.appticket.dto.event.EventUpdateDTO;
import com.codegym.appticket.dto.home.HomeEventDTO;
import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IEventService {
    Page<EventDTO> findAll(Pageable pageable);

    Page<HomeEventDTO> findAllEvent(Pageable pageable);

    Page<EventDTO> search(EventSearchDTO dto, Pageable pageable);

    EventDTO findById(Long id);

    EventDTO create(EventCreateDTO dto);

    EventDTO update(Long id, EventUpdateDTO dto);

    void delete(Long id);

    List<TrendingEventDTO> findTopTrendingEvents();
    List<UpComingEventDTO> findUpComingEvents();
}
