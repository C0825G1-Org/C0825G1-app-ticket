package com.codegym.appticket.controller;

import com.codegym.appticket.entity.QRCode;
import com.codegym.appticket.entity.Ticket;
import com.codegym.appticket.service.ITicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {
    
    private final ITicketService ticketService;

    // Record để làm key gộp vé (Dùng String statusName để so sánh chính xác tuyệt đối)
    public record TicketGroupKey(Long eventId, String eventTitle, String eventLocation, String statusName) {}

    // 1. Danh sách vé của tôi (đã gộp theo sự kiện và trạng thái)
    @GetMapping("/my-tickets")
    public String myTickets(Model model) {
        String email = getCurrentUserEmail();
        if (email == null) {
            return "redirect:/login";
        }
        
        com.codegym.appticket.entity.User currentUser = ticketService.getUserByEmail(email);
        Long userId = currentUser.getId();
        
        List<Ticket> tickets = ticketService.getTicketsByUserId(userId);
        
        // Gộp vé theo sự kiện và trạng thái
        java.util.Map<TicketGroupKey, List<Ticket>> groupedTickets = tickets.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        t -> new TicketGroupKey(
                                t.getBookingDetail().getTicketType().getEvent().getId(),
                                t.getBookingDetail().getTicketType().getEvent().getTitle(),
                                t.getBookingDetail().getTicketType().getEvent().getLocation(),
                                t.getBookingDetail().getBooking().getStatus().name()
                        ),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        
        model.addAttribute("groupedTickets", groupedTickets);
        return "ticket/list";
    }

    // 2. Chi tiết các vé của một sự kiện (có lọc theo trạng thái)
    @GetMapping("/event-detail/{eventId}")
    public String eventDetail(@PathVariable Long eventId, 
                             @org.springframework.web.bind.annotation.RequestParam(required = false) com.codegym.appticket.entity.BookingStatus status, 
                             Model model) {
        String email = getCurrentUserEmail();
        if (email == null) {
            return "redirect:/login";
        }
        
        com.codegym.appticket.entity.User currentUser = ticketService.getUserByEmail(email);
        Long userId = currentUser.getId();
        
        List<Ticket> tickets = ticketService.getTicketsByUserIdAndEventId(userId, eventId);
        
        // Lọc vé theo trạng thái nếu có
        if (status != null) {
            tickets = tickets.stream()
                    .filter(t -> t.getBookingDetail().getBooking().getStatus() == status)
                    .collect(java.util.stream.Collectors.toList());
        }

        if (tickets.isEmpty()) {
            return "redirect:/tickets/my-tickets";
        }
        
        List<Long> ticketIds = tickets.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toList());
        List<QRCode> qrCodes = ticketService.getQRCodesByTicketIds(ticketIds);
        
        // Tạo map để dễ tra cứu QR code theo ticket id
        java.util.Map<Long, QRCode> qrCodeMap = qrCodes.stream()
                .collect(java.util.stream.Collectors.toMap(qr -> qr.getTicket().getId(), qr -> qr));
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("qrCodeMap", qrCodeMap);
        model.addAttribute("event", tickets.get(0).getBookingDetail().getTicketType().getEvent());
        
        return "ticket/detail";
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else if (principal instanceof com.codegym.appticket.config.CustomOAuth2User) {
            return ((com.codegym.appticket.config.CustomOAuth2User) principal).getEmail();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            return ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
        }
        
        return authentication.getName();
    }

    // 2. Chi tiết vé & Hiển thị QR Code (Tích hợp)
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Ticket ticket = ticketService.getTicketById(id);
        QRCode qrCode = ticketService.getQRCodeByTicketId(id);
        
        model.addAttribute("ticket", ticket);
        model.addAttribute("qrCode", qrCode);
        return "ticket/detail";
    }
}
