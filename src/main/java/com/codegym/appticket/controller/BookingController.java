package com.codegym.appticket.controller;

import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.TicketType;
import com.codegym.appticket.service.IBookingService;
import com.codegym.appticket.service.IVnPayService;
import com.codegym.appticket.entity.Location;

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
                          @RequestParam(required = false) Long occurrence,
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

        // Filter tickets if occurrence is selected
        com.codegym.appticket.entity.EventOccurrence selectedOccurrence = null;
        if (occurrence != null) {
            ticketTypes = ticketTypes.stream()
                    .filter(tt -> tt.getEventOccurrence().getId().equals(occurrence))
                    .toList();
            
            if (event.getEventOccurrences() != null) {
                selectedOccurrence = event.getEventOccurrences().stream()
                        .filter(oc -> oc.getId().equals(occurrence))
                        .findFirst()
                        .orElse(null);
            }
        } 
        
        // Fallback or default logic if no specific occurrence selected or not found
        if (selectedOccurrence == null && event.getEventOccurrences() != null && !event.getEventOccurrences().isEmpty()) {
            // If we have tickets after filtering (or not filtering), try to use the occurrence of the first ticket
            if (!ticketTypes.isEmpty()) {
                 selectedOccurrence = ticketTypes.get(0).getEventOccurrence();
            } else {
                 // Fallback to first available occurrence of event
                 selectedOccurrence = event.getEventOccurrences().get(0);
            }
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

        // Fix location display based on selected occurrence
        String location = "Chưa cập nhật";
        if (selectedOccurrence != null) {
            Location loc = selectedOccurrence.getLocation();
            if (loc != null) {
                StringBuilder fullAddress = new StringBuilder(loc.getAddressDetail());
                if (loc.getWard() != null) {
                    fullAddress.append(", ").append(loc.getWard().getName());
                    if (loc.getWard().getProvince() != null) {
                        fullAddress.append(", ").append(loc.getWard().getProvince().getName());
                    }
                }
                location = fullAddress.toString();
            }
            model.addAttribute("startTime", selectedOccurrence.getStartTime());
        }
        model.addAttribute("location", location);

        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("preSelectedQuantities", preSelectedQuantities);
        
        // Calculate available quantity for each ticket type (quantity - sold)
        Map<Long, Integer> ticketAvailability = new HashMap<>();
        for (TicketType tt : ticketTypes) {
            int sold = bookingService.getSoldQuantity(tt.getId());
            int available = tt.getQuantity() - sold;
            ticketAvailability.put(tt.getId(), Math.max(0, available)); // Ensure non-negative
        }
        model.addAttribute("ticketAvailability", ticketAvailability);
        
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
                        if (tt != null)
                            selectedTickets.put(tt, quantity);
                    }
                }
            }
        }

        if (selectedTickets.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một loại vé.");
            return "redirect:/bookings/book/" + eventId;
        }

        model.addAttribute("event", event);

        // Fix location display logic: Use occurrence from the selected tickets
        String location = "Chưa cập nhật";
        com.codegym.appticket.entity.EventOccurrence occurrence = null;
        
        // Get occurrence from the first selected ticket
        if (!selectedTickets.isEmpty()) {
            occurrence = selectedTickets.keySet().iterator().next().getEventOccurrence();
        }

        // Fallback to first occurrence if for some reason we missed it (shouldn't happen with valid tickets)
        if (occurrence == null && event.getEventOccurrences() != null && !event.getEventOccurrences().isEmpty()) {
            occurrence = event.getEventOccurrences().get(0);
        }

        if (occurrence != null) {
            Location loc = occurrence.getLocation();
            if (loc != null) {
                StringBuilder fullAddress = new StringBuilder(loc.getAddressDetail());
                if (loc.getWard() != null) {
                    fullAddress.append(", ").append(loc.getWard().getName());
                    if (loc.getWard().getProvince() != null) {
                        fullAddress.append(", ").append(loc.getWard().getProvince().getName());
                    }
                }
                location = fullAddress.toString();
            }
            model.addAttribute("startTime", occurrence.getStartTime());
        }
        model.addAttribute("location", location);

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
            List<com.codegym.appticket.entity.BookingDetail> details = bookingService
                    .getBookingDetailsByBookingId(bookingId);

            if (details.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy chi tiết đặt vé.");
                return "redirect:/tickets/my-tickets";
            }

            Map<TicketType, Integer> selectedTickets = new java.util.HashMap<>();
            for (com.codegym.appticket.entity.BookingDetail detail : details) {
                selectedTickets.put(detail.getTicketType(), detail.getQuantity());
            }

            com.codegym.appticket.entity.EventOccurrence occurrence = details.get(0).getTicketType().getEventOccurrence();
            model.addAttribute("event", occurrence.getEvent());
            model.addAttribute("startTime", occurrence.getStartTime());

            String location = "Chưa cập nhật";
            if (occurrence.getLocation() != null) {
                Location loc = occurrence.getLocation();
                StringBuilder fullAddress = new StringBuilder(loc.getAddressDetail());
                if (loc.getWard() != null) {
                    fullAddress.append(", ").append(loc.getWard().getName());
                    if (loc.getWard().getProvince() != null) {
                        fullAddress.append(", ").append(loc.getWard().getProvince().getName());
                    }
                }
                location = fullAddress.toString();
            }
            model.addAttribute("location", location);
            model.addAttribute("selectedTickets", selectedTickets);
            // Pass the existing booking ID to the view so we can reuse it
            model.addAttribute("bookingId", bookingId);


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
            @RequestParam(required = false) Long bookingId,
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes,
            jakarta.servlet.http.HttpServletRequest request) {
        String userEmail = getCurrentUserEmail();

        if (userEmail == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đặt vé");
            return "redirect:/login";
        }

        try {
            com.codegym.appticket.entity.User currentUser = bookingService.getUserByEmail(userEmail);
            Long userId = currentUser.getId();
            Booking booking;

            // CHECK: If bookingId exists, validate and reuse it
            if (bookingId != null) {
                booking = bookingService.getBookingById(bookingId);
                // Security check: Ensure current user owns this booking
                if (!booking.getUser().getId().equals(userId)) {
                    throw new RuntimeException("Bạn không có quyền thanh toán booking này.");
                }
                // Status check: Ensure it is PENDING
                if (booking.getStatus() != com.codegym.appticket.entity.BookingStatus.PENDING) {
                    // If already SUCCESS, redirect to success page directly?
                    if (booking.getStatus() == com.codegym.appticket.entity.BookingStatus.SUCCESS) {
                        return "redirect:/bookings/success/" + booking.getId();
                    }
                    throw new RuntimeException("Booking không ở trạng thái chờ thanh toán.");
                }
            } else {
                // OLD FLOW: Create new booking
                Map<Long, Integer> ticketQuantities = new HashMap<>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (entry.getKey().startsWith("ticket_")) {
                        try {
                            Long ttId = Long.parseLong(entry.getKey().replace("ticket_", ""));
                            if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                                Integer qty = Integer.parseInt(entry.getValue());
                                if (qty > 0) {
                                    ticketQuantities.put(ttId, qty);
                                }
                            }
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
                booking = bookingService.createBooking(eventId, userId, ticketQuantities);
            }

            long totalAmount = bookingService.calculateTotalAmount(booking.getId());
            String paymentUrl = vnPayService.createPaymentUrl(request, booking.getId(), totalAmount);

            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/book/" + eventId;
        }
    }

    // 4. Trang Kết quả thành công
    @GetMapping("/success/{id}")
    public String success(@PathVariable Long id, Model model) {
        Booking booking = bookingService.getBookingById(id);
        java.util.List<com.codegym.appticket.entity.Ticket> tickets = bookingService.getTicketsByBookingId(id);

        // Lấy thông tin sự kiện từ vé đầu tiên (vì 1 booking thường cho 1 sự kiện)
        if (!tickets.isEmpty()) {
            model.addAttribute("event",
                    tickets.get(0).getBookingDetail().getTicketType().getEventOccurrence().getEvent());
            model.addAttribute("startTime",
                    tickets.get(0).getBookingDetail().getTicketType().getEventOccurrence().getStartTime());
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
