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
import com.codegym.appticket.entity.EventStatus;
import org.springframework.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/user/events")
@RequiredArgsConstructor
public class UserEventController {

    private final IEventService eventService;

    private final IEventCategoryService eventCategoryService;
    private final IUserRepository userRepository;
    private final Validator validator;

    @Value("${tinymce.api-key}")
    private String tinyMceApiKey;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        String email = auth.getName();
        if (auth.getPrincipal() instanceof UserInfoUserDetails) {
            email = ((UserInfoUserDetails) auth.getPrincipal()).getUsername();
        } else if (auth.getPrincipal() instanceof com.codegym.appticket.config.CustomOAuth2User) {
            email = ((com.codegym.appticket.config.CustomOAuth2User) auth.getPrincipal()).getEmail();
        }

        return userRepository.findByEmailAndNotDeleted(email);
    }

    @GetMapping
    public String listMyEvents(@PageableDefault(size = 5) Pageable pageable, Model model) {
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    org.springframework.data.domain.Sort
                            .by(org.springframework.data.domain.Sort.Direction.ASC, "status")
                            .and(org.springframework.data.domain.Sort
                                    .by(org.springframework.data.domain.Sort.Direction.DESC, "createdDate")));
        }
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
        User currentUser = getCurrentUser();
        if (currentUser == null)
            return "redirect:/login";

        model.addAttribute("eventCreateDTO", new EventCreateDTO());
        model.addAttribute("categories", eventCategoryService.findAll());
        model.addAttribute("tinyMceApiKey", tinyMceApiKey);
        return "user/event/create";
    }

    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> createEvent(@ModelAttribute EventCreateDTO createDTO,
            BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        // Manual Validation if NOT Draft
        if (createDTO.getStatus() != EventStatus.DRAFT) {
            validator.validate(createDTO, bindingResult);
        }

        if (bindingResult.hasErrors()) {
            response.put("status", "validation_error");
            response.put("errors", getValidationErrors(bindingResult));
            return response;
        }

        try {
            User currentUser = getCurrentUser();
            if (currentUser == null)
                throw new RuntimeException("Bạn cần đăng nhập để thực hiện chức năng này");

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
        User currentUser = getCurrentUser();
        if (currentUser == null)
            return "redirect:/login";

        EventDTO eventDTO = eventService.findById(id);

        // Security Check: Only allow if Current User is the Organizer
        if (eventDTO.getOrganizerId() == null || !eventDTO.getOrganizerId().equals(currentUser.getId())) {
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

        updateDTO.setOrganizerId(eventDTO.getOrganizerId()); // Keep implementation simple

        model.addAttribute("eventUpdateDTO", updateDTO);
        model.addAttribute("eventId", id);
        model.addAttribute("categories", eventCategoryService.findAll());
        model.addAttribute("tinyMceApiKey", tinyMceApiKey);

        return "user/event/edit";
    }

    @PostMapping("/edit/{id}")
    @ResponseBody
    public Map<String, Object> updateEvent(@PathVariable Long id, @ModelAttribute EventUpdateDTO updateDTO,
            BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        // Manual Validation if NOT Draft
        if (updateDTO.getStatus() != EventStatus.DRAFT) {
            validator.validate(updateDTO, bindingResult);
        }

        if (bindingResult.hasErrors()) {
            response.put("status", "validation_error");
            response.put("errors", getValidationErrors(bindingResult));
            return response;
        }

        try {
            User currentUser = getCurrentUser();
            if (currentUser == null)
                throw new RuntimeException("Unauthorized");

            // Verify ownership again
            EventDTO existingEvent = eventService.findById(id);
            if (existingEvent.getOrganizerId() == null || !existingEvent.getOrganizerId().equals(currentUser.getId())) {
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

    private Map<String, String> getValidationErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }

    @GetMapping("/detail/{id}")
    public String showDetail(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null)
            return "redirect:/login";

        EventDTO eventDTO = eventService.findById(id);

        // Security Check: Only allow if Current User is the Organizer
        if (eventDTO.getOrganizerId() == null || !eventDTO.getOrganizerId().equals(currentUser.getId())) {
            return "redirect:/user/events?error=unauthorized";
        }

        // Add additional data if needed for detail view
        // For example, flattened variables for simpler Thymeleaf access
        model.addAttribute("event", eventDTO);

        // Find specific media types for easier access
        model.addAttribute("bannerUrl", eventDTO.getEventMedias().stream()
                .filter(m -> m.getMediaPurpose().name().equals("BANNER")).findFirst()
                .map(m -> m.getMediaUrl()).orElse(null));

        model.addAttribute("logoUrl", eventDTO.getEventMedias().stream()
                .filter(m -> m.getMediaPurpose().name().equals("LOGO")).findFirst()
                .map(m -> m.getMediaUrl()).orElse(null));

        return "user/event/detail";
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        try {
            EventDTO eventDTO = eventService.findById(id);
            User currentUser = getCurrentUser();
            if (!eventDTO.getOrganizerId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("Bạn không có quyền xóa sự kiện này.");
            }

            eventService.delete(id); // Soft delete
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelEvent(@PathVariable Long id, @RequestParam(required = true) String reason) {
        try {
            EventDTO eventDTO = eventService.findById(id);
            User currentUser = getCurrentUser();
            if (currentUser == null)
                return ResponseEntity.status(401).body("Unauthorized");

            if (!eventDTO.getOrganizerId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("Bạn không có quyền hủy sự kiện này.");
            }

            eventService.cancel(id, reason);
            return ResponseEntity.ok(Map.of("message", "Đã hủy sự kiện thành công!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage(), "status", "error"));
        }
    }

    @PostMapping("/{id}/publish")
    @ResponseBody
    public ResponseEntity<?> publishEvent(@PathVariable Long id) {
        try {
            EventDTO eventDTO = eventService.findById(id);
            User currentUser = getCurrentUser();
            if (currentUser == null)
                return ResponseEntity.status(401).body("Unauthorized");

            if (!eventDTO.getOrganizerId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("Bạn không có quyền thao tác trên sự kiện này.");
            }

            eventService.submitForApproval(id);
            return ResponseEntity.ok(Map.of("message", "Gửi duyệt thành công!", "status", "success"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "status", "error"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage(), "status", "error"));
        }
    }
}
