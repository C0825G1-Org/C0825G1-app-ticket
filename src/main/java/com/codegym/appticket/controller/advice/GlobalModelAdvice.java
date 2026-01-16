package com.codegym.appticket.controller.advice;

import com.codegym.appticket.entity.EventStatus;
import com.codegym.appticket.repository.IEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final IEventRepository eventRepository;

    private final jakarta.servlet.http.HttpServletRequest request;

    @ModelAttribute("pendingEventCount")
    public long getPendingEventCount() {
        if (request.getRequestURI().contains("/stream")) {
            return 0;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
             try {
                return eventRepository.countByStatus(EventStatus.PENDING);
             } catch (Exception e) {
                 return 0;
             }
        }
        return 0;
    }
}
