package com.codegym.appticket.controller.api;

import com.codegym.appticket.dto.event.EventNotificationDTO;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.EventStatus;
import com.codegym.appticket.repository.IEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationRestController {

    private final IEventRepository eventRepository;
    private final com.codegym.appticket.service.impl.AdminNotificationService adminNotificationService;

    @GetMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamNotifications() {
        return adminNotificationService.subscribe();
    }

    @GetMapping("/pending-events")
    public ResponseEntity<List<EventNotificationDTO>> getPendingEvents() {
        List<Event> pendingEvents = eventRepository.findTop10ByStatusOrderByCreatedDateDesc(EventStatus.PENDING);
        
        List<EventNotificationDTO> dtos = pendingEvents.stream().map(event -> {
            EventNotificationDTO dto = new EventNotificationDTO();
            dto.setId(event.getId());
            dto.setTitle(event.getTitle());
            dto.setCreatedBy(event.getCreatedBy() != null ? event.getCreatedBy().getFullName() : "Unknown");
            dto.setCreatedDate(event.getCreatedDate());
            dto.setTimeAgo(calculateTimeAgo(event.getCreatedDate()));
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> getPendingCount() {
        return ResponseEntity.ok(eventRepository.countByStatus(EventStatus.PENDING));
    }

    private String calculateTimeAgo(LocalDateTime createdDate) {
        if (createdDate == null) return "Vừa xong";
        Duration duration = Duration.between(createdDate, LocalDateTime.now());
        long seconds = duration.getSeconds();
        
        if (seconds < 60) return "Vừa xong";
        if (seconds < 3600) return (seconds / 60) + " phút trước";
        if (seconds < 86400) return (seconds / 3600) + " giờ trước";
        return (seconds / 86400) + " ngày trước";
    }
}
