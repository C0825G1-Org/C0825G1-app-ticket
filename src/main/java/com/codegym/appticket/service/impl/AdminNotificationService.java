package com.codegym.appticket.service.impl;

import com.codegym.appticket.dto.event.EventNotificationDTO;
import com.codegym.appticket.entity.Event;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AdminNotificationService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // Timeout 30 minutes, or infinite depending on requirement
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        emitters.add(emitter);
        return emitter;
    }

    public void sendNotification(Event event) {
        EventNotificationDTO dto = new EventNotificationDTO(
                event.getId(),
                event.getTitle(),
                event.getCreatedBy() != null ? event.getCreatedBy().getFullName() : "Unknown",
                event.getCreatedDate(),
                "Vừa xong"
        );

        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("NEW_EVENT_REQUEST").data(dto));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });
        emitters.removeAll(deadEmitters);
    }
}
