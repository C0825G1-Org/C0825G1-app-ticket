package com.codegym.appticket.controller;

import com.codegym.appticket.service.IBookingService;
import com.codegym.appticket.service.IVnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final IVnPayService vnPayService;
    private final IBookingService bookingService;

    @GetMapping("/vn-pay-callback")
    public String payCallbackHandler(HttpServletRequest request, Model model) {
        String status = request.getParameter("vnp_ResponseCode");
        String orderId = request.getParameter("vnp_TxnRef");
        
        // Validate signature
        int checksum = vnPayService.orderReturn(request);
        
        if (checksum == 1) {

             try {
                // Parse bookingId from "bookingId_timestamp" format
                String[] parts = orderId.split("_");
                Long bookingId = Long.parseLong(parts[0]);
                
                String transactionNo = request.getParameter("vnp_TransactionNo");
                bookingService.confirmBooking(bookingId, transactionNo);
                return "redirect:/bookings/success/" + bookingId;
             } catch (Exception e) {
                return "redirect:/"; // Or error page
             }
        } else {
             if (orderId != null && !orderId.isEmpty()) {
                 try {
                     // Parse bookingId from "bookingId_timestamp" format
                     String[] parts = orderId.split("_");
                     Long bookingId = Long.parseLong(parts[0]);
                     
                    bookingService.cancelBooking(bookingId);
                 } catch (Exception e) {
                     // Log error
                 }
             }
             return "booking/payment_failed"; 
        }
    }
}
