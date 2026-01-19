package com.codegym.appticket.controller;


import com.codegym.appticket.dto.home.HomeEventDTO;
import com.codegym.appticket.dto.home.NearByEventDTO;
import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.repository.IEventRepository;
import com.codegym.appticket.service.IEventCategoryService;
import com.codegym.appticket.service.IEventService;
import com.codegym.appticket.service.IGeoLocationService;
import jakarta.servlet.http.HttpServletRequest;
import com.codegym.appticket.dto.home.LocationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@Slf4j
public class HomeController {
    
    @Autowired
    private IEventService eventService;
    
    @Autowired
    private IEventCategoryService eventCategoryService;
    
    @Autowired
    private IEventRepository eventRepository;

    @Autowired
    private IGeoLocationService geoLocationService;

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
            @RequestParam(required = false) String location,
            Model model) {
        
        // Show all approved events without filters
        Page<HomeEventDTO> events = eventService.findAllEvent(page, size);

        model.addAttribute("categories", eventCategoryService.findAll());
        model.addAttribute("events", events);
        model.addAttribute("nearbyEvents", getNearbyEvents(null, 6));
        
        return "home/event";
    }

//    @GetMapping("/event/search")
//    public String searchEvent(
//            @RequestParam(required = false) String search,
//            @RequestParam(required = false) Long category,
//            @RequestParam(required = false) String location,
//            @RequestParam(required = false) String sort,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "6") int size,
//            Model model) {
//
//        // Normalize empty strings to null
//        if (search != null && search.trim().isEmpty()) {
//            search = null;
//        }
//        if (location != null && location.trim().isEmpty()) {
//            location = null;
//        }
//
//        // Use unified search method that returns HomeEventDTO
//        Page<HomeEventDTO> events = eventService.searchHomeEvents(search, category, location, page, size, sort);
//
//        // Load all categories for filter sidebar
//        model.addAttribute("categories", eventCategoryService.findAll());
//        model.addAttribute("events", events);
//
//        return "home/event";
//    }
    @GetMapping("/event/search")
    public String searchEvent(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            HttpServletRequest request,  // Thêm để lấy IP
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
        model.addAttribute("nearbyEvents", getNearbyEvents(location, 6));

        return "home/event";
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // Nếu là localhost, trả về empty string để ip-api.com tự detect
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            ip = ""; // ip-api.com sẽ tự động phát hiện IP public
        }
        
        // Nếu có nhiều IP (qua nhiều proxy), lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
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

    private List<NearByEventDTO> getNearbyEvents(String location, int limit) {
        log.info("=== DEBUG getNearbyEvents: location = '{}' ===", location);
        
        // Nếu không có location hoặc là "Toàn quốc", trả về danh sách rỗng
        if (location == null || location.trim().isEmpty() || location.equals("Toàn quốc")) {
            log.info("Location is null/empty/Toàn quốc, returning empty list");
            return List.of(); // Trả về danh sách rỗng
        }

        // Lấy tọa độ từ tên địa điểm
        double[] coordinates = geoLocationService.getCoordinates(location);

        if (coordinates == null) {
            log.warn("Không tìm thấy tọa độ cho địa điểm: {}", location);
            return List.of();
        }
        
        log.info("Tọa độ cho '{}': latitude={}, longitude={}", location, coordinates[0], coordinates[1]);

        // Lấy sự kiện gần
        List<NearByEventDTO> nearbyEvents = eventService.findNearbyEvents(
                coordinates[0], // latitude
                coordinates[1], // longitude
                limit
        );
        
        log.info("Tìm thấy {} sự kiện gần '{}'", nearbyEvents.size(), location);
        nearbyEvents.forEach(event -> 
            log.info("  - Event: {} | Location: {} | Distance: {} km", 
                event.getTitle(), event.getLocation(), event.getDistance())
        );

        return nearbyEvents;
    }
}
