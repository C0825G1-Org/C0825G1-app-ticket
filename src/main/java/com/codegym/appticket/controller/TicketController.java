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

    // 1. Danh sách vé của tôi
    @GetMapping("/my-tickets")
    public String myTickets(Model model) {
        String email = getCurrentUserEmail();
        if (email == null) {
            return "redirect:/login";
        }
        
        com.codegym.appticket.entity.User currentUser = ticketService.getUserByEmail(email);
        Long userId = currentUser.getId();
        
        List<Ticket> tickets = ticketService.getTicketsByUserId(userId);
        model.addAttribute("tickets", tickets);
        return "ticket/list";
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
