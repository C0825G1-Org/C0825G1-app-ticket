package com.codegym.appticket.config;

import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.EventStatus;
import com.codegym.appticket.repository.IEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventStatusScheduler {

    private final IEventRepository eventRepository;

    // Run every minute
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void updateEventStatuses() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Update APPROVED -> HAPPENING
        List<Event> startedEvents = eventRepository.findStartedEvents(now);
        if (!startedEvents.isEmpty()) {
            for (Event event : startedEvents) {
                event.setStatus(EventStatus.HAPPENING);
            }
            eventRepository.saveAll(startedEvents);
            log.info("Updated status to HAPPENING for {} events.", startedEvents.size());
        }

        // 2. Update HAPPENING/APPROVED -> FINISHED
        List<Event> finishedEvents = eventRepository.findFinishedEvents(now);
        if (!finishedEvents.isEmpty()) {
            for (Event event : finishedEvents) {
                event.setStatus(EventStatus.FINISHED);
            }
            eventRepository.saveAll(finishedEvents);
            log.info("Updated status to FINISHED for {} events.", finishedEvents.size());
        }
    }
}
