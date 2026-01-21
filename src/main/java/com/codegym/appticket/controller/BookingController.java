package com.codegym.appticket.controller;

import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.TicketType;
import com.codegym.appticket.service.IBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public String showForm(@PathVariable Long eventId, 
                          @RequestParam Map<String, String> params,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        String email = getCurrentUserEmail();
        Event event;
        List<TicketType> ticketTypes;
        try {
            event = bookingService.getEventById(eventId);
            ticketTypes = bookingService.getTicketTypesByEventId(eventId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sự kiện hoặc sự kiện đã bị hủy.");
            return "redirect:/";
        }

        if (ticketTypes == null || ticketTypes.isEmpty()) {
            model.addAttribute("warning", "Sự kiện hiện chưa có vé để đặt.");
        }

        if (email != null) {
            try {
                com.codegym.appticket.entity.User currentUser = bookingService.getUserByEmail(email);
                model.addAttribute("currentUser", currentUser);
            } catch (Exception e) {
            }
        }

        // Extract pre-selected ticket quantities from URL parameters
        Map<Long, Integer> preSelectedQuantities = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("ticket_")) {
                try {
                    Long ticketTypeId = Long.parseLong(entry.getKey().replace("ticket_", ""));
                    Integer quantity = Integer.parseInt(entry.getValue());
                    if (quantity > 0) {
                        preSelectedQuantities.put(ticketTypeId, quantity);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid parameters
                }
            }
        }
        model.addAttribute("event", event);
        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("preSelectedQuantities", preSelectedQuantities);
        return "booking/form";
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return email;
        } else if (principal instanceof com.codegym.appticket.config.CustomOAuth2User) {
            String email = ((com.codegym.appticket.config.CustomOAuth2User) principal).getEmail();
            return email;
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            String email = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
            return email;
        }
        
        return authentication.getName();
    }

    // 2. Trang Xác nhận đặt vé
    @PostMapping("/confirm")
    public String confirm(@RequestParam Long eventId,
                          @RequestParam Map<String, String> params,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        String email = getCurrentUserEmail();
        
        Event event = bookingService.getEventById(eventId);
        Map<TicketType, Integer> selectedTickets = new HashMap<>();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("ticket_")) {
                Long ticketTypeId = Long.parseLong(entry.getKey().replace("ticket_", ""));
                if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    Integer quantity = Integer.parseInt(entry.getValue());
                    if (quantity > 0) {
                        TicketType tt = bookingService.getTicketTypesByEventId(eventId).stream()
                                .filter(t -> t.getId().equals(ticketTypeId))
                                .findFirst().orElse(null);
                        if (tt != null) selectedTickets.put(tt, quantity);
                    }
                }
            }
        }

        if (selectedTickets.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một loại vé.");
            return "redirect:/bookings/book/" + eventId;
        }

        model.addAttribute("event", event);
        model.addAttribute("selectedTickets", selectedTickets);

        // Lấy thông tin người dùng hiện tại để hiển thị trên trang xác nhận
        if (email != null) {
            try {
                com.codegym.appticket.entity.User currentUser = bookingService.getUserByEmail(email);
                model.addAttribute("currentUser", currentUser);
            } catch (Exception e) {
            }
        } else {
        }

        return "booking/confirm";
    }

    @GetMapping("/confirm")
    public String confirm(@RequestParam Long bookingId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.getBookingById(bookingId);
            List<com.codegym.appticket.entity.BookingDetail> details = bookingService.getBookingDetailsByBookingId(bookingId);
            
            if (details.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy chi tiết đặt vé.");
                return "redirect:/tickets/my-tickets";
            }
            
            Map<TicketType, Integer> selectedTickets = new java.util.HashMap<>();
            for (com.codegym.appticket.entity.BookingDetail detail : details) {
                selectedTickets.put(detail.getTicketType(), detail.getQuantity());
            }
            
            model.addAttribute("event", details.get(0).getTicketType().getEvent());
            model.addAttribute("selectedTickets", selectedTickets);
            
            String email = getCurrentUserEmail();
            if (email != null) {
                com.codegym.appticket.entity.User currentUser = bookingService.getUserByEmail(email);
                model.addAttribute("currentUser", currentUser);
            }
            
            return "booking/confirm";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/tickets/my-tickets";
        }
    }

    // 3. Xử lý Lưu đặt vé
    @PostMapping("/save")
    public String save(@RequestParam Long eventId,
                       @RequestParam Map<String, String> params,
                       RedirectAttributes redirectAttributes,
                       jakarta.servlet.http.HttpServletRequest request) {
        String userEmail = getCurrentUserEmail();
        
        if (userEmail == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đặt vé");
            return "redirect:/login";
        }
        
        Map<Long, Integer> ticketQuantities = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("ticket_")) {
                Long ttId = Long.parseLong(entry.getKey().replace("ticket_", ""));
                if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    Integer qty = Integer.parseInt(entry.getValue());
                    if (qty > 0) {
                        ticketQuantities.put(ttId, qty);
                    }
                }
            }
        }

        try {
            com.codegym.appticket.entity.User currentUser = bookingService.getUserByEmail(userEmail);
            Long userId = currentUser.getId();
            
            Booking booking = bookingService.createBooking(eventId, userId, ticketQuantities);
            
            long totalAmount = bookingService.calculateTotalAmount(booking.getId());
            
            String paymentUrl = vnPayService.createPaymentUrl(request, booking.getId(), totalAmount);
            
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/book/" + eventId;
        }
    }

    @GetMapping("/success/{id}")
    public String success(@PathVariable Long id, Model model) {
        Booking booking = bookingService.getBookingById(id);
        java.util.List<com.codegym.appticket.entity.Ticket> tickets = bookingService.getTicketsByBookingId(id);
        
        // Lấy thông tin sự kiện từ vé đầu tiên (vì 1 booking thường cho 1 sự kiện)
        if (!tickets.isEmpty()) {
            model.addAttribute("event", tickets.get(0).getBookingDetail().getTicketType().getEvent());
        }
        
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
