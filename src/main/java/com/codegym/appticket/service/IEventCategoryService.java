package com.codegym.appticket.service;

import com.codegym.appticket.entity.EventCategory;
import java.util.List;

public interface IEventCategoryService {
    List<EventCategory> findAll();
}
