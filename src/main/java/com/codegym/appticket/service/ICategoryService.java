package com.codegym.appticket.service;

import com.codegym.appticket.entity.EventCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICategoryService {
    List<EventCategory> getAllCategories();
    EventCategory findById(Long id);
    Boolean save(EventCategory eventCategory);
    Boolean delete(Long id);
}
