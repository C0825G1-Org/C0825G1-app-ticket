package com.codegym.appticket.controller.api;

import com.codegym.appticket.dto.event.EventStatsDTO;
import com.codegym.appticket.entity.User;
import com.codegym.appticket.service.IEventService;
import com.codegym.appticket.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.codegym.appticket.dto.event.EventDTO;

@RestController
@RequestMapping("/api/organizer/events")
@RequiredArgsConstructor
public class OrganizerEventRestController {

    private final IEventService eventService;
    private final IUserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String email = auth.getName();
        if (auth.getPrincipal() instanceof com.codegym.appticket.config.CustomOAuth2User) {
            email = ((com.codegym.appticket.config.CustomOAuth2User) auth.getPrincipal()).getEmail();
        } else if (auth.getPrincipal() instanceof com.codegym.appticket.dto.user.UserInfoUserDetails) {
            email = ((com.codegym.appticket.dto.user.UserInfoUserDetails) auth.getPrincipal()).getUsername();
        }
        return userRepository.findByEmailAndNotDeleted(email);
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getEventStats(@PathVariable Long id, @RequestParam(required = false) Long occurrenceId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        EventDTO event = eventService.findById(id);
        if (event.getOrganizerId() == null || !event.getOrganizerId().equals(currentUser.getId())) {
             return ResponseEntity.status(403).body("Access Denied");
        }

        EventStatsDTO stats = eventService.getEventStats(id, occurrenceId);
        return ResponseEntity.ok(stats);
    }
}
