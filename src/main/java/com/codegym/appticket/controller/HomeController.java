package com.codegym.appticket.controller;

import com.codegym.appticket.dto.event.EventDTO;
import com.codegym.appticket.dto.event.EventSearchDTO;
import com.codegym.appticket.dto.home.HomeEventDTO;
import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.service.IEventCategoryService;
import com.codegym.appticket.service.IEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {
    
    @Autowired
    private IEventService eventService;
    
    @Autowired
    private IEventCategoryService eventCategoryService;

    @GetMapping("/")
    public String showHomePage(Model model) {
        // Lấy top 3 trending events cho section "Bảng Xếp Hạng"
        List<TrendingEventDTO> trendingEvents = eventService.findTopTrendingEvents();
        model.addAttribute("trendingEvents", trendingEvents);

        // Lấy top 4 upcoming events cho section "Sự kiện Sắp tới"
        List<UpComingEventDTO> upcomingEvents = eventService.findUpComingEvents();
        model.addAttribute("upcomingEvents", upcomingEvents);

        return "home/index";
    }

    @GetMapping("/events")
    public String showEventPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model) {
        
        // Show all approved events without filters
        Pageable pageable = PageRequest.of(page, size);
        Page<HomeEventDTO> events = eventService.findAllEvent(pageable);
        
        // Load all categories for filter sidebar
        model.addAttribute("categories", eventCategoryService.findAll());
        model.addAttribute("events", events);
        
        return "home/event";
    }

    @GetMapping("/event/search")
    public String searchEvent(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model) {
        
        // Create search DTO with filters
        EventSearchDTO searchDTO = new EventSearchDTO();
        searchDTO.setTitle(search);
        searchDTO.setCategoryId(category);
        
        // TODO: Add location filtering when EventSearchDTO supports it
        // For now, location is received but not used in search
        
        // Create pageable with sorting
        // TODO: Implement sorting logic based on sort parameter
        Pageable pageable = PageRequest.of(page, size);
        
        // Search events with filters
        Page<EventDTO> events;
        if (search != null || category != null || location != null) {
            events = eventService.search(searchDTO, pageable);
        } else {
            // If no filters, redirect to /events
            return "redirect:/events";
        }
        
        // Load all categories for filter sidebar
        model.addAttribute("categories", eventCategoryService.findAll());
        model.addAttribute("events", events);
        
        return "home/event";
    }
}
