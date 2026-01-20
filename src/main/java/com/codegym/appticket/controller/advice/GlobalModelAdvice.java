package com.codegym.appticket.controller.advice;

import com.codegym.appticket.entity.EventStatus;
import com.codegym.appticket.repository.IEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
@RequiredArgsConstructor
public class GlobalModelAdvice {

    // Use Service instead of Repository for better Transaction management
    private final com.codegym.appticket.service.IEventService eventService; // Assuming count method exists or added
    private final com.codegym.appticket.repository.IEventRepository eventRepository; // Keep for now if service logic missing

    private final jakarta.servlet.http.HttpServletRequest request;

    @Autowired
    private com.codegym.appticket.service.IBookingService bookingService;

    private long cachedCount = 0;
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION = 10000; // 10 seconds

    @ModelAttribute("pendingEventCount")
    public long getPendingEventCount() {
        String uri = request.getRequestURI();
        if (uri.matches(".*\\.(css|js|png|jpg|jpeg|gif|ico|woff|woff2|ttf|svg)$")) {
            return 0;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !(authentication instanceof AnonymousAuthenticationToken)) {
                
                boolean isStaffOrAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("STAFF"));
                
                if (isStaffOrAdmin) {
                     long now = System.currentTimeMillis();
                     if (now - lastCacheTime > CACHE_DURATION) {
                         cachedCount = eventRepository.countByStatus(EventStatus.PENDING);
                         lastCacheTime = now;
                     }
                     return cachedCount;
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching pending event count: " + e.getMessage());
            return 0;
        }
        return 0;
    }



}
