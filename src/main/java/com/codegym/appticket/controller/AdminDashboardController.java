package com.codegym.appticket.controller;

import com.codegym.appticket.entity.EventStatus;
import com.codegym.appticket.repository.IBookingDetailRepository;
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

    private final IBookingDetailRepository bookingDetailRepository;
    private final IEventRepository eventRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 1. Stats
        BigDecimal totalRevenue = bookingDetailRepository.sumTotalRevenue();
        Long totalTicketsSold = bookingDetailRepository.countTotalTicketsSold();

        long pendingEventsCount = eventRepository.countByStatus(EventStatus.PENDING);

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalTicketsSold", totalTicketsSold);
        model.addAttribute("pendingEventsCount", pendingEventsCount);
        // Note: New events could be defined as created this month, but for now using
        // pending count as a proxy for 'attention needed' or just approved count for
        // 'active'.
        // Let's use Approved count as "New Events" metric label in UI or just "Total
        // Active Events".
        // The UI demo says "New Events", let's map it to Pending for now as that's
        // actionable.

        // 2. Pending Events Table (Top 5)
        model.addAttribute("pendingEvents", eventRepository.findByStatus(EventStatus.PENDING, PageRequest.of(0, 5)));

        return "admin/dashboard";
    }
}
