package com.codegym.appticket.controller;

import com.codegym.appticket.entity.EventStatus;

import com.codegym.appticket.repository.IEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final IEventRepository eventRepository;
    private final com.codegym.appticket.repository.IBookingRepository bookingRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 1. Revenue Today (Cash Flow)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        java.time.LocalDateTime endOfDay = now.toLocalDate().atTime(java.time.LocalTime.MAX);

        BigDecimal revenueToday = bookingRepository.sumTotalRevenue(startOfDay, endOfDay);
        if (revenueToday == null)
            revenueToday = BigDecimal.ZERO;

        // 2. Live Status (Happening Events)
        long happeningEventsCount = eventRepository.countByStatus(EventStatus.HAPPENING);

        // 3. Pending Events Count (Action Required)
        long pendingEventsCount = eventRepository.countByStatus(EventStatus.PENDING);

        // 4. Upcoming Events Sales Performance (Top 3)
        var upcomingSales = eventRepository.findTopUpcomingEventsWithSales(3);

        // 5. Recent Bookings (Last 5 Successful)
        var recentBookings = bookingRepository
                .findRecentBookings(com.codegym.appticket.entity.BookingStatus.SUCCESS);

        // Add to model
        model.addAttribute("revenueToday", revenueToday);
        model.addAttribute("happeningEventsCount", happeningEventsCount);
        model.addAttribute("pendingEventsCount", pendingEventsCount);
        model.addAttribute("upcomingSales", upcomingSales);
        model.addAttribute("recentBookings", recentBookings);

        // 6. Pending Events Table (Top 5)
        model.addAttribute("pendingEvents", eventRepository.findByStatus(EventStatus.PENDING, PageRequest.of(0, 5)));

        model.addAttribute("activeNav", "dashboard");
        return "admin/dashboard";
    }
}
