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
        
        if (checksum == 1) { // Success & Valid Signature
             // Check status code again just to be safe, though orderReturn checks it too? 
             // orderReturn returns 1 only if responseCode is "00" AND signature is valid.
             
             Long bookingId = Long.parseLong(orderId);
             bookingService.confirmBooking(bookingId);
             return "redirect:/bookings/success/" + bookingId;
        } else {
             // Failed or Invalid Signature
             // If checksum is 0 (Failed) or -1 (Invalid)
             if (orderId != null && !orderId.isEmpty()) {
                 try {
                    Long bookingId = Long.parseLong(orderId);
                    bookingService.cancelBooking(bookingId);
                 } catch (NumberFormatException e) {
                     // Log error
                 }
             }
             return "booking/payment_failed"; 
        }
    }
}
