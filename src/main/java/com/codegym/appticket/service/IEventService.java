package com.codegym.appticket.service;

import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.entity.Event;

import java.util.List;

public interface IEventService {
    List<TrendingEventDTO> findTopTrendingEvents();
    List<UpComingEventDTO> findUpComingEvents();
}
