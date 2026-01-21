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
import com.codegym.appticket.repository.IUserRepository;
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
    private final IUserRepository userRepository;

    @Value("${tinymce.api-key}")
    private String tinyMceApiKey;

    @GetMapping
    public String listEvents(@ModelAttribute("eventSearchDTO") EventSearchDTO searchDTO,
            @PageableDefault(size = 5) Pageable pageable,
            Model model) {
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    org.springframework.data.domain.Sort
                            .by(org.springframework.data.domain.Sort.Direction.ASC, "status")
                            .and(org.springframework.data.domain.Sort
                                    .by(org.springframework.data.domain.Sort.Direction.DESC, "createdDate")));
        }

        // Unified search (if DTO has empty fields, it acts as findAll because
        // repository handles nulls)
        // But we need to make sure empty string becomes null for title if repo checks
        // NULL
        if (searchDTO.getTitle() != null && searchDTO.getTitle().isEmpty())
            searchDTO.setTitle(null);

        model.addAttribute("events", eventService.search(searchDTO, pageable));

        // Add statistics
        // Add statistics
        model.addAttribute("totalEvents", eventService.countByStatuses(
                java.util.Arrays.asList(EventStatus.HAPPENING, EventStatus.PENDING, EventStatus.APPROVED,
                        EventStatus.REJECTED, EventStatus.CANCELLED)));
        model.addAttribute("pendingCount", eventService.countByStatus(EventStatus.PENDING));
        model.addAttribute("approvedCount", eventService.countByStatus(EventStatus.APPROVED));
        model.addAttribute("happeningCount", eventService.countByStatus(EventStatus.HAPPENING));
        model.addAttribute("rejectedCount", eventService.countByStatuses(
                java.util.Arrays.asList(EventStatus.REJECTED, EventStatus.CANCELLED)));

        model.addAttribute("categories", eventCategoryService.findAll());
        // Pass EventStatus values for filter dropdown
        model.addAttribute("statuses", EventStatus.values());

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

    @GetMapping("/{id}/modal")
    public String getEventDetailModal(@PathVariable Long id, Model model) {
        try {
            EventDTO event = eventService.findById(id);
            model.addAttribute("event", event);
            return "admin/event/detail :: detailModalContent";
        } catch (RuntimeException e) {
            return "error/404 :: content"; // Or empty
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
    public ResponseEntity<?> rejectEvent(@PathVariable Long id, @RequestParam(required = true) String reason) {
        try {
            eventService.reject(id, reason);
            return ResponseEntity.ok(Map.of("message", "Đã từ chối sự kiện!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage(), "status", "error"));
        }
    }

    @PostMapping("/{id}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelEvent(@PathVariable Long id, @RequestParam(required = true) String reason) {
        try {
            eventService.cancel(id, reason);
            return ResponseEntity.ok(Map.of("message", "Đã hủy sự kiện thành công!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage(), "status", "error"));
        }
    }

    @PostMapping("/{id}/restore")
    @ResponseBody
    public Map<String, Object> restoreEvent(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            eventService.restore(id);
            response.put("status", "success");
            response.put("message", "Đã khôi phục sự kiện thành công!");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }
}
