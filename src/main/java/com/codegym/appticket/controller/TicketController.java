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

import java.util.List;

@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final ITicketService ticketService;

    // 1. Danh sách vé của tôi
    @GetMapping("/my-tickets")
    public String myTickets(Model model) {
        // Giả lập lấy user theo email (Thay cho ID cố định)
        String mockEmail = "tranlegianguyen97dn@gmail.com";
        com.codegym.appticket.entity.User mockUser = ticketService.getUserByEmail(mockEmail);
        Long userId = mockUser.getId();
        
        List<Ticket> tickets = ticketService.getTicketsByUserId(userId);
        model.addAttribute("tickets", tickets);
        return "ticket/list";
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
