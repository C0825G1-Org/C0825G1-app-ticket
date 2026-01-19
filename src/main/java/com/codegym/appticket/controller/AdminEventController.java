package com.codegym.appticket.controller;

import com.codegym.appticket.dto.event.EventCreateDTO;
import com.codegym.appticket.dto.event.EventDTO;

import com.codegym.appticket.dto.event.EventMediaDTO;
import com.codegym.appticket.dto.event.EventOccurrenceDTO;
import com.codegym.appticket.dto.event.EventSearchDTO;
import com.codegym.appticket.dto.event.EventUpdateDTO;
import com.codegym.appticket.dto.event.TicketTypeDTO;
import com.codegym.appticket.entity.EventStatus;
import com.codegym.appticket.entity.MediaPurpose;

import com.codegym.appticket.service.IEventCategoryService;
import com.codegym.appticket.service.IEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final IEventService eventService;
    private final IEventCategoryService eventCategoryService;
    private final com.codegym.appticket.repository.IUserRepository userRepository;

    @Value("${tinymce.api-key}")
    private String tinyMceApiKey;

    @GetMapping
    public String listEvents(@ModelAttribute("eventSearchDTO") EventSearchDTO searchDTO,
            @RequestParam(required = false) EventStatus status,
            @PageableDefault(size = 5) Pageable pageable,
            Model model) {
        if (status != null) {
            model.addAttribute("events", eventService.findByStatus(status, pageable));
        } else if (searchDTO.getTitle() != null || searchDTO.getCategoryId() != null ||
                searchDTO.getStartDate() != null || searchDTO.getEndDate() != null) {
            model.addAttribute("events", eventService.search(searchDTO, pageable));
        } else {
            model.addAttribute("events", eventService.findAll(pageable));
        }

        // Add statistics
        model.addAttribute("totalEvents", eventService.countAll());
        model.addAttribute("pendingCount", eventService.countByStatus(EventStatus.PENDING));
        model.addAttribute("approvedCount", eventService.countByStatus(EventStatus.APPROVED));
        model.addAttribute("rejectedCount", eventService.countByStatus(EventStatus.REJECTED));

        model.addAttribute("categories", eventCategoryService.findAll());
        return "admin/event/list";

    }

    @GetMapping("/{id}")
    public String showEventDetail(@PathVariable Long id, Model model) {
        try {
            EventDTO event = eventService.findById(id);
            model.addAttribute("event", event);
            return "admin/event/detail";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error/404";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        EventCreateDTO dto = new EventCreateDTO();
        dto.getEventOccurrences().add(new EventOccurrenceDTO());
        // dto.getTicketTypes().add(new TicketTypeDTO()); // Khởi tạo tùy chọn nếu cần

        model.addAttribute("eventCreateDTO", dto);
        model.addAttribute("categories", eventCategoryService.findAll());
        model.addAttribute("organizers", userRepository.findOrganizers());
        model.addAttribute("tinyMceApiKey", tinyMceApiKey);
        return "admin/event/create";
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createEvent(
            @Valid @ModelAttribute("eventCreateDTO") EventCreateDTO dto,
            BindingResult bindingResult) {

        Map<String, Object> response = new HashMap<>();

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            response.put("status", "validation_error");
            response.put("errors", errors);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            dto.setStatus(EventStatus.APPROVED);
            EventDTO createdEvent = eventService.create(dto);
            response.put("status", "success");
            response.put("message", "Tạo sự kiện thành công!");
            response.put("redirectUrl", "/admin/events/" + createdEvent.getId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            EventDTO eventDTO = eventService.findById(id);
            EventUpdateDTO updateDTO = new EventUpdateDTO();
            updateDTO.setTitle(eventDTO.getTitle());
            updateDTO.setDescription(eventDTO.getDescription());
            updateDTO.setCategoryId(eventDTO.getCategoryId());
            updateDTO.setStatus(eventDTO.getStatus());

            if (eventDTO.getEventOccurrences() != null) {
                updateDTO.setEventOccurrences(new ArrayList<>(eventDTO.getEventOccurrences()));
            } else {
                updateDTO.getEventOccurrences().add(new EventOccurrenceDTO());
            }

            if (eventDTO.getTicketTypes() != null && !eventDTO.getTicketTypes().isEmpty()) {
                updateDTO.setTicketTypes(new ArrayList<>(eventDTO.getTicketTypes()));
            } else {
                updateDTO.getTicketTypes().add(TicketTypeDTO.builder().quantity(1).build());
            }

            if (eventDTO.getEventMedias() != null) {
                // Ánh xạ media vào các trường URL cụ thể
                List<String> galleryUrls = new ArrayList<>();
                for (EventMediaDTO media : eventDTO.getEventMedias()) {
                    if (media.getMediaPurpose() == MediaPurpose.BANNER) {
                        updateDTO.setBannerUrl(media.getMediaUrl());
                    } else if (media.getMediaPurpose() == MediaPurpose.LOGO) {
                        updateDTO.setLogoUrl(media.getMediaUrl());
                    } else if (media.getMediaPurpose() == MediaPurpose.TICKET_MAP) {
                        updateDTO.setTicketMapUrl(media.getMediaUrl());
                    } else if (media.getMediaPurpose() == MediaPurpose.GALLERY) {
                        galleryUrls.add(media.getMediaUrl());
                    }
                }
                updateDTO.setGalleryUrls(galleryUrls);
                // Giữ eventMedias nếu view cần, nhưng view đã refactor sử dụng URL
                updateDTO.setEventMedias(new ArrayList<>(eventDTO.getEventMedias()));
            }

            model.addAttribute("eventUpdateDTO", updateDTO);
            model.addAttribute("eventId", id);
            model.addAttribute("categories", eventCategoryService.findAll());
            model.addAttribute("organizers", userRepository.findOrganizers());
            if (updateDTO.getOrganizerId() == null && eventDTO.getOrganizerId() != null) {
                updateDTO.setOrganizerId(eventDTO.getOrganizerId());
            }
            model.addAttribute("tinyMceApiKey", tinyMceApiKey);
            return "admin/event/edit";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/events";
        }
    }

    @PostMapping("/edit/{id}")
    @ResponseBody
    public ResponseEntity<?> updateEvent(
            @PathVariable Long id,
            @Valid @ModelAttribute("eventUpdateDTO") EventUpdateDTO dto,
            BindingResult bindingResult) {

        Map<String, Object> response = new HashMap<>();

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            response.put("status", "validation_error");
            response.put("errors", errors);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            eventService.update(id, dto);
            response.put("status", "success");
            response.put("message", "Cập nhật sự kiện thành công!");
            response.put("redirectUrl", "/admin/events/" + id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveEvent(@PathVariable Long id) {
        try {
            eventService.approve(id);
            // Optionally notification logic
            return ResponseEntity.ok(Map.of("message", "Đã duyệt sự kiện thành công!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage(), "status", "error"));
        }
    }

    @PostMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectEvent(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            eventService.reject(id, reason);
            // Optionally notification logic
            return ResponseEntity.ok(Map.of("message", "Đã từ chối sự kiện!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage(), "status", "error"));
        }
    }
}
