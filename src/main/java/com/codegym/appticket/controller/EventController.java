package com.codegym.appticket.controller;

import com.codegym.appticket.dto.event.EventCreateDTO;
import com.codegym.appticket.dto.event.EventDTO;

import com.codegym.appticket.dto.event.EventTimeDTO;
import com.codegym.appticket.dto.event.EventUpdateDTO;
import com.codegym.appticket.dto.event.TicketTypeDTO;
import com.codegym.appticket.service.IEventCategoryService;
import com.codegym.appticket.service.IEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class EventController {

    private final IEventService eventService;
    private final IEventCategoryService eventCategoryService;

    @GetMapping
    public String listEvents(@ModelAttribute("eventSearchDTO") com.codegym.appticket.dto.event.EventSearchDTO searchDTO,
            @org.springframework.data.web.PageableDefault(size = 5) org.springframework.data.domain.Pageable pageable,
            Model model) {
        if (searchDTO.getTitle() != null || searchDTO.getCategoryId() != null ||
                searchDTO.getStartDate() != null || searchDTO.getEndDate() != null) {
            model.addAttribute("events", eventService.search(searchDTO, pageable));
        } else {
            model.addAttribute("events", eventService.findAll(pageable));
        }
        model.addAttribute("categories", eventCategoryService.findAll());
        return "event/list";
    }

    @GetMapping("/{id}")
    public String showEventDetail(@PathVariable Long id, Model model) {
        try {
            EventDTO event = eventService.findById(id);
            model.addAttribute("event", event);
            return "event/detail";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error/404";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        EventCreateDTO dto = new EventCreateDTO();
        // Init lists to ensure Thymeleaf binding availability
        dto.getEventTimes().add(new EventTimeDTO());
        // dto.getTicketTypes().add(new TicketTypeDTO()); // Optional init if needed

        model.addAttribute("eventCreateDTO", dto);
        model.addAttribute("categories", eventCategoryService.findAll());
        return "event/create";
    }

    @PostMapping("/create")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> createEvent(
            @Valid @ModelAttribute("eventCreateDTO") EventCreateDTO dto,
            BindingResult bindingResult) {

        java.util.Map<String, Object> response = new java.util.HashMap<>();

        if (bindingResult.hasErrors()) {
            java.util.Map<String, String> errors = new java.util.HashMap<>();
            for (org.springframework.validation.FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            response.put("status", "validation_error");
            response.put("errors", errors);
            return org.springframework.http.ResponseEntity.badRequest().body(response);
        }

        try {
            EventDTO createdEvent = eventService.create(dto);
            response.put("status", "success");
            response.put("message", "Tạo sự kiện thành công!");
            response.put("redirectUrl", "/admin/events/" + createdEvent.getId());
            return org.springframework.http.ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            EventDTO eventDTO = eventService.findById(id);
            EventUpdateDTO updateDTO = new EventUpdateDTO();
            updateDTO.setTitle(eventDTO.getTitle());
            updateDTO.setDescription(eventDTO.getDescription());
            updateDTO.setLocation(eventDTO.getLocation());
            updateDTO.setCategoryId(eventDTO.getCategoryId());
            updateDTO.setStatus(eventDTO.getStatus());

            if (eventDTO.getEventTimes() != null) {
                updateDTO.setEventTimes(new java.util.ArrayList<>(eventDTO.getEventTimes()));
            } else {
                updateDTO.getEventTimes().add(new EventTimeDTO());
            }

            // Load Ticket Types
            if (eventDTO.getTicketTypes() != null && !eventDTO.getTicketTypes().isEmpty()) {
                updateDTO.setTicketTypes(new java.util.ArrayList<>(eventDTO.getTicketTypes()));
            } else {
                updateDTO.getTicketTypes().add(TicketTypeDTO.builder().quantity(1).build());
            }

            if (eventDTO.getEventMedias() != null) {
                // Map media to specific URL fields
                java.util.List<String> galleryUrls = new java.util.ArrayList<>();
                for (com.codegym.appticket.dto.event.EventMediaDTO media : eventDTO.getEventMedias()) {
                    if (media.getMediaPurpose() == com.codegym.appticket.entity.MediaPurpose.BANNER) {
                        updateDTO.setBannerUrl(media.getMediaUrl());
                    } else if (media.getMediaPurpose() == com.codegym.appticket.entity.MediaPurpose.LOGO) {
                        updateDTO.setLogoUrl(media.getMediaUrl());
                    } else if (media.getMediaPurpose() == com.codegym.appticket.entity.MediaPurpose.TICKET_MAP) {
                        updateDTO.setTicketMapUrl(media.getMediaUrl());
                    } else if (media.getMediaPurpose() == com.codegym.appticket.entity.MediaPurpose.GALLERY) {
                        galleryUrls.add(media.getMediaUrl());
                    }
                }
                updateDTO.setGalleryUrls(galleryUrls);
                // Keep eventMedias if needed by view, but view refactor uses URLs now
                updateDTO.setEventMedias(new java.util.ArrayList<>(eventDTO.getEventMedias()));
            }

            model.addAttribute("eventUpdateDTO", updateDTO);
            model.addAttribute("eventId", id);
            model.addAttribute("categories", eventCategoryService.findAll());
            return "event/edit";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/events";
        }
    }

    @PostMapping("/edit/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> updateEvent(
            @PathVariable Long id,
            @Valid @ModelAttribute("eventUpdateDTO") EventUpdateDTO dto,
            BindingResult bindingResult) {

        java.util.Map<String, Object> response = new java.util.HashMap<>();

        if (bindingResult.hasErrors()) {
            java.util.Map<String, String> errors = new java.util.HashMap<>();
            for (org.springframework.validation.FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            response.put("status", "validation_error");
            response.put("errors", errors);
            return org.springframework.http.ResponseEntity.badRequest().body(response);
        }

        try {
            eventService.update(id, dto);
            response.put("status", "success");
            response.put("message", "Cập nhật sự kiện thành công!");
            response.put("redirectUrl", "/admin/events/" + id);
            return org.springframework.http.ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
