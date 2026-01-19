package com.codegym.appticket.controller;

import com.codegym.appticket.dto.event.EventCreateDTO;
import com.codegym.appticket.dto.event.EventDTO;
import com.codegym.appticket.dto.event.EventUpdateDTO;
import com.codegym.appticket.dto.user.UserInfoUserDetails;
import com.codegym.appticket.entity.Event;

import com.codegym.appticket.entity.User;
import com.codegym.appticket.service.IEventCategoryService;
import com.codegym.appticket.service.IEventService;
import com.codegym.appticket.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/user/events")
@RequiredArgsConstructor
public class UserEventController {

    private final IEventService eventService;

    private final IEventCategoryService eventCategoryService;
    private final IUserRepository userRepository;

    @Value("${tinymce.api-key}")
    private String tinyMceApiKey;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserInfoUserDetails) {
                return ((UserInfoUserDetails) principal).getUser();
            } else if (principal instanceof com.codegym.appticket.config.CustomOAuth2User) {
                String email = ((com.codegym.appticket.config.CustomOAuth2User) principal).getEmail();
                return userRepository.findByEmail(email).orElse(null);
            }
        }
        String email = auth != null ? auth.getName() : null;
        if (email == null)
            return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    @GetMapping
    public String listMyEvents(@PageableDefault(size = 5) Pageable pageable, Model model) {
        User currentUser = getCurrentUser();
        // If user not found (should be handled by security), redirect or error
        if (currentUser == null)
            return "redirect:/login";

        Page<Event> events = eventService.findEventsByOrganizer(currentUser, pageable);
        model.addAttribute("events", events);
        return "user/event/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("eventCreateDTO", new EventCreateDTO());
        model.addAttribute("categories", eventCategoryService.findAll());
        model.addAttribute("tinyMceApiKey", tinyMceApiKey);
        return "user/event/create";
    }

    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> createEvent(@ModelAttribute EventCreateDTO createDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Organizer is set to current user in Service if DTO organizerId is null.
            // But we should explicit it or ensure Service handles it.
            // Service Logic: if dto.organizerId is null -> Default to Creator.
            // In UserEventController, we don't pass organizerId in form, so it is null.
            // So Creator (User) becomes Organizer. Correct.

            eventService.create(createDTO);
            response.put("status", "success");
            response.put("redirectUrl", "/user/events");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        EventDTO eventDTO = eventService.findById(id);
        User currentUser = getCurrentUser();

        // Security Check: Only allow if Current User is the Organizer
        if (!eventDTO.getOrganizerId().equals(currentUser.getId())) {
            return "redirect:/user/events?error=unauthorized";
        }

        EventUpdateDTO updateDTO = new EventUpdateDTO();
        updateDTO.setTitle(eventDTO.getTitle());
        updateDTO.setDescription(eventDTO.getDescription());
        // updateDTO.setLocation(eventDTO.getLocation()); // Removed location
        updateDTO.setCategoryId(eventDTO.getCategoryId());
        updateDTO.setStatus(eventDTO.getStatus());
        updateDTO.setBannerUrl(
                eventDTO.getEventMedias().stream().filter(m -> m.getMediaPurpose().name().equals("BANNER")).findFirst()
                        .map(m -> m.getMediaUrl()).orElse(null));
        updateDTO.setLogoUrl(eventDTO.getEventMedias().stream().filter(m -> m.getMediaPurpose().name().equals("LOGO"))
                .findFirst().map(m -> m.getMediaUrl()).orElse(null));
        updateDTO.setTicketMapUrl(
                eventDTO.getEventMedias().stream().filter(m -> m.getMediaPurpose().name().equals("TICKET_MAP"))
                        .findFirst().map(m -> m.getMediaUrl()).orElse(null));
        updateDTO.setGalleryUrls(
                eventDTO.getEventMedias().stream().filter(m -> m.getMediaPurpose().name().equals("GALLERY"))
                        .map(m -> m.getMediaUrl()).collect(java.util.stream.Collectors.toList()));
        updateDTO.setEventOccurrences(eventDTO.getEventOccurrences());
        updateDTO.setTicketTypes(eventDTO.getTicketTypes());
        updateDTO.setOrganizerId(eventDTO.getOrganizerId()); // Keep implementation simple

        model.addAttribute("eventUpdateDTO", updateDTO);
        model.addAttribute("eventId", id);
        model.addAttribute("categories", eventCategoryService.findAll());
        model.addAttribute("tinyMceApiKey", tinyMceApiKey);

        return "user/event/edit";
    }

    @PostMapping("/edit/{id}")
    @ResponseBody
    public Map<String, Object> updateEvent(@PathVariable Long id, @ModelAttribute EventUpdateDTO updateDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Verify ownership again
            EventDTO existingEvent = eventService.findById(id);
            User currentUser = getCurrentUser();
            if (!existingEvent.getOrganizerId().equals(currentUser.getId())) {
                response.put("status", "error");
                response.put("message", "Unauthorized access.");
                return response;
            }

            // Force Organizer to NOT change (or keep as is)
            updateDTO.setOrganizerId(currentUser.getId());

            eventService.update(id, updateDTO);
            response.put("status", "success");
            response.put("redirectUrl", "/user/events");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        try {
            EventDTO eventDTO = eventService.findById(id);
            User currentUser = getCurrentUser();
            if (!eventDTO.getOrganizerId().equals(currentUser.getId())) {
                return org.springframework.http.ResponseEntity.status(403).body("Bạn không có quyền xóa sự kiện này.");
            }

            eventService.delete(id); // Soft delete
            return org.springframework.http.ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
