package com.codegym.appticket.service;

import com.codegym.appticket.dto.event.EventCreateDTO;
import com.codegym.appticket.dto.event.EventDTO;
import com.codegym.appticket.dto.event.EventUpdateDTO;

import java.util.List;

public interface IEventService {
    org.springframework.data.domain.Page<EventDTO> findAll(org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<EventDTO> search(com.codegym.appticket.dto.event.EventSearchDTO dto,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<EventDTO> findByStatus(com.codegym.appticket.entity.EventStatus status,
            org.springframework.data.domain.Pageable pageable);

    EventDTO findById(Long id);

    EventDTO create(EventCreateDTO dto);

    EventDTO update(Long id, EventUpdateDTO dto);

    void delete(Long id);
}
