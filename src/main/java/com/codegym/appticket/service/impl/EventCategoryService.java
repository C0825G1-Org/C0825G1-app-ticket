package com.codegym.appticket.service.impl;

import com.codegym.appticket.entity.EventCategory;
import com.codegym.appticket.repository.IEventCategoryRepository;
import com.codegym.appticket.service.IEventCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventCategoryService implements IEventCategoryService {

    private final IEventCategoryRepository eventCategoryRepository;

    @Override
    public List<EventCategory> findAll() {
        return eventCategoryRepository.findAll();
    }
}
