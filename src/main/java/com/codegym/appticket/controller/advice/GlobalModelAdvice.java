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

    @ModelAttribute("pendingEventCount")
    public long getPendingEventCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication instanceof AnonymousAuthenticationToken)) {
            // Check if user has admin/staff authority to avoid unnecessary DB calls for normal users
            boolean isStaffOrAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("STAFF"));
            
            if (isStaffOrAdmin) {
                 return eventRepository.countByStatus(EventStatus.PENDING);
            }
        }
        return 0;
    }


    @ModelAttribute("currentUser")
    public com.codegym.appticket.entity.User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication instanceof AnonymousAuthenticationToken)) {
            
            Object principal = authentication.getPrincipal();
            String email = null;
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof com.codegym.appticket.config.CustomOAuth2User) {
                email = ((com.codegym.appticket.config.CustomOAuth2User) principal).getEmail();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                email = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
            } else {
                email = authentication.getName();
            }

            if (email != null) {
                try {
                    return bookingService.getUserByEmail(email);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}
