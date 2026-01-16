package com.codegym.appticket.controller;

import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.TicketType;
import com.codegym.appticket.service.IBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final IBookingService bookingService;
    private final com.codegym.appticket.service.IVnPayService vnPayService;

    // 1. Trang Form đặt vé
    @GetMapping("/book/{eventId}")
    public String showForm(@PathVariable Long eventId, Model model) {
        Event event = bookingService.getEventById(eventId);
        List<TicketType> ticketTypes = bookingService.getTicketTypesByEventId(eventId);
        
        model.addAttribute("event", event);
        model.addAttribute("ticketTypes", ticketTypes);
        return "booking/form";
    }

    // 2. Trang Xác nhận đặt vé
    @PostMapping("/confirm")
    public String confirm(@RequestParam Long eventId,
                          @RequestParam Map<String, String> params,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        Event event = bookingService.getEventById(eventId);
        Map<TicketType, Integer> selectedTickets = new HashMap<>();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("ticket_")) {
                Long ticketTypeId = Long.parseLong(entry.getKey().replace("ticket_", ""));
                Integer quantity = Integer.parseInt(entry.getValue());
                if (quantity > 0) {
                    TicketType tt = bookingService.getTicketTypesByEventId(eventId).stream()
                            .filter(t -> t.getId().equals(ticketTypeId))
                            .findFirst().orElse(null);
                    if (tt != null) selectedTickets.put(tt, quantity);
                }
            }
        }

        if (selectedTickets.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một loại vé.");
            return "redirect:/bookings/book/" + eventId;
        }

        model.addAttribute("event", event);
        model.addAttribute("selectedTickets", selectedTickets);
        return "booking/confirm";
    }

    // 3. Xử lý Lưu đặt vé
    @PostMapping("/save")
    public String save(@RequestParam Long eventId,
                       @RequestParam Map<String, String> params,
                       RedirectAttributes redirectAttributes,
                       jakarta.servlet.http.HttpServletRequest request) {
        // Giả lập lấy user theo email
        String mockEmail = "nguyenns6802@gmail.com";
        
        Map<Long, Integer> ticketQuantities = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("ticket_")) {
                Long ttId = Long.parseLong(entry.getKey().replace("ticket_", ""));
                Integer qty = Integer.parseInt(entry.getValue());
                ticketQuantities.put(ttId, qty);
            }
        }

        try {
            com.codegym.appticket.entity.User mockUser = bookingService.getUserByEmail(mockEmail);
            Long userId = mockUser.getId();
            
            Booking booking = bookingService.createBooking(eventId, userId, ticketQuantities);
            
            long totalAmount = bookingService.calculateTotalAmount(booking.getId());
            String paymentUrl = vnPayService.createPaymentUrl(request, booking.getId(), totalAmount);
            
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/book/" + eventId;
        }
    }

    // 4. Trang Kết quả thành công
    @GetMapping("/success/{id}")
    public String success(@PathVariable Long id, Model model) {
        Booking booking = bookingService.getBookingById(id);
        java.util.List<com.codegym.appticket.entity.Ticket> tickets = bookingService.getTicketsByBookingId(id);
        
        model.addAttribute("booking", booking);
        model.addAttribute("tickets", tickets);
        return "booking/success";
    }

    // 5. Xử lý Hủy vé
    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("message", "Hủy vé thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/tickets/my-tickets";
    }
}
