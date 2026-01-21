package com.codegym.appticket.controller;

import com.codegym.appticket.entity.QRCode;
import com.codegym.appticket.entity.Ticket;
import com.codegym.appticket.service.ITicketService;
import com.codegym.appticket.entity.User;
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

    // 1. Danh sách vé của tôi (đã gộp theo sự kiện)
    @GetMapping("/my-tickets")
    public String myTickets(Model model) {
        String email = getCurrentUserEmail();
        if (email == null) {
            return "redirect:/login";
        }

        com.codegym.appticket.entity.User currentUser = ticketService.getUserByEmail(email);
        Long userId = currentUser.getId();
        
        List<Ticket> tickets = ticketService.getTicketsByUserId(userId);
        
        // Gộp vé theo sự kiện
        java.util.Map<com.codegym.appticket.entity.Event, List<Ticket>> groupedTickets = tickets.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        t -> t.getBookingDetail().getTicketType().getEvent(),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        
        model.addAttribute("groupedTickets", groupedTickets);
        return "ticket/list";
    }

    // 2. Chi tiết các vé của một sự kiện
    @GetMapping("/event-detail/{eventId}")
    public String eventDetail(@PathVariable Long eventId, Model model) {
        String email = getCurrentUserEmail();
        if (email == null) {
            return "redirect:/login";
        }
        
        com.codegym.appticket.entity.User currentUser = ticketService.getUserByEmail(email);
        Long userId = currentUser.getId();
        
        List<Ticket> tickets = ticketService.getTicketsByUserIdAndEventId(userId, eventId);
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
