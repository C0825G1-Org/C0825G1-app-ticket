package com.codegym.appticket.controller;

import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.service.IEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/")
public class HomeController {
    
    @Autowired
    private IEventService eventService;

    @GetMapping
    public String showHomePage(Model model) {
        // Lấy top 3 trending events cho section "Bảng Xếp Hạng"
        List<TrendingEventDTO> trendingEvents = eventService.findTopTrendingEvents();
        model.addAttribute("trendingEvents", trendingEvents);

        // Lấy top 4 upcoming events cho section "Sự kiện Sắp tới"
        List<UpComingEventDTO> upcomingEvents = eventService.findUpComingEvents();
        model.addAttribute("upcomingEvents", upcomingEvents);

        return "home/index";
    }
}
