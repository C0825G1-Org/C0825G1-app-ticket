package com.codegym.appticket.service.impl;

import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.repository.IEventRepository;
import com.codegym.appticket.service.IEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService implements IEventService {

    @Autowired
    private IEventRepository eventRepository;

    @Override
    public List<TrendingEventDTO> findTopTrendingEvents() {
        return eventRepository.findTopTrendingEvents();
    }

    @Override
    public List<UpComingEventDTO> findUpComingEvents() {
        return eventRepository.findUpComingEvents();
    }

//    @Override
//    public List<UpComingEventDTO> findUpcomingEvents() {
//        return eventRepository.findUpComingEvents();
//    }
}
