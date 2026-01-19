package com.codegym.appticket.controller;


import com.codegym.appticket.dto.home.HomeEventDTO;
import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.repository.IEventRepository;
import com.codegym.appticket.service.IEventCategoryService;
import com.codegym.appticket.service.IEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {
    
    @Autowired
    private IEventService eventService;
    
    @Autowired
    private IEventCategoryService eventCategoryService;
    
    @Autowired
    private IEventRepository eventRepository;

    @GetMapping("/")
    public String showHomePage(Model model) {
        // Lấy top 3 trending events cho section "Bảng Xếp Hạng"
        List<TrendingEventDTO> trendingEvents = eventService.findTopTrendingEvents();
        model.addAttribute("trendingEvents", trendingEvents);

        // Lấy top 4 upcoming events cho section "Sự kiện Sắp tới"
        List<UpComingEventDTO> upcomingEvents = eventService.findUpComingEvents();
        model.addAttribute("upcomingEvents", upcomingEvents);
        
        // Load categories for search dropdown
        model.addAttribute("categories", eventCategoryService.findAll());

        return "home/index";
    }

    @GetMapping("/events")
    public String showEventPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model) {
        
        // Show all approved events without filters
        Page<HomeEventDTO> events = eventService.findAllEvent(page, size);

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
        
        // Normalize empty strings to null
        if (search != null && search.trim().isEmpty()) {
            search = null;
        }
        if (location != null && location.trim().isEmpty()) {
            location = null;
        }
        
        // Use unified search method that returns HomeEventDTO
        Page<HomeEventDTO> events = eventService.searchHomeEvents(search, category, location, page, size, sort);
        
        // Load all categories for filter sidebar
        model.addAttribute("categories", eventCategoryService.findAll());
        model.addAttribute("events", events);
        
        return "home/event";
    }

    @GetMapping("/event/{id}")
    public String showEventDetail(@PathVariable Long id, Model model) {
        // Fetch event details
        var eventDetail = eventRepository.findEventDetailById(id);
        
        if (eventDetail == null) {
            // Event not found or not approved
            return "redirect:/events";
        }
        
        // Fetch ticket types with available quantities
        var ticketTypes = eventRepository.findTicketTypesByEventId(id);
        
        model.addAttribute("event", eventDetail);
        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("currentPage", "event-detail");
        
        return "home/event_detail";
    }

    @GetMapping("/contact")
    public String contact() {
        return "home/contact";
    }

    @GetMapping("/403")
    public String showAccessDenied() {
        return "error/403";
    }
}
