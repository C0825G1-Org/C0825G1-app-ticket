package com.codegym.appticket.service.impl;

import com.codegym.appticket.dto.event.EventCreateDTO;
import com.codegym.appticket.dto.event.EventDTO;
import com.codegym.appticket.dto.event.EventMediaDTO;
import com.codegym.appticket.dto.event.EventTimeDTO;
import com.codegym.appticket.dto.event.EventUpdateDTO;
import com.codegym.appticket.dto.event.TicketTypeDTO;
import com.codegym.appticket.dto.home.HomeEventDTO;
import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.EventCategory;
import com.codegym.appticket.entity.EventMedia;
import com.codegym.appticket.entity.EventStatus;
import com.codegym.appticket.entity.EventTime;
import com.codegym.appticket.repository.IEventCategoryRepository;
import com.codegym.appticket.repository.IEventMediaRepository;
import com.codegym.appticket.repository.IEventRepository;
import com.codegym.appticket.repository.IEventTimeRepository;
import com.codegym.appticket.service.IEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService implements IEventService {

        private final IEventRepository eventRepository;
        private final IEventCategoryRepository eventCategoryRepository;
        private final IEventTimeRepository eventTimeRepository;
        private final IEventMediaRepository eventMediaRepository;
        private final com.codegym.appticket.repository.ITicketTypeRepository ticketTypeRepository;

        @Override
        public org.springframework.data.domain.Page<EventDTO> findAll(
                        org.springframework.data.domain.Pageable pageable) {
                return eventRepository.findAll(pageable).map(this::convertToDTO);
        }

    @Override
    public Page<HomeEventDTO> findAllEvent(Pageable pageable) {
        return eventRepository.findAllEvent(pageable);
    }

    @Override
        public org.springframework.data.domain.Page<EventDTO> search(com.codegym.appticket.dto.event.EventSearchDTO dto,
                        org.springframework.data.domain.Pageable pageable) {
                java.time.LocalDateTime start = dto.getStartDate() != null ? dto.getStartDate().atStartOfDay() : null;
                java.time.LocalDateTime end = dto.getEndDate() != null
                                ? dto.getEndDate().atTime(java.time.LocalTime.MAX)
                                : null;

                return eventRepository.searchEvents(
                                dto.getTitle(),
                                dto.getCategoryId(),
                                start,
                                end,
                                pageable).map(this::convertToDTO);
        }

        @Override
        public EventDTO findById(Long id) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + id));
                return convertToDTO(event);
        }

        @Override
        @Transactional
        public EventDTO create(EventCreateDTO dto) {
                EventCategory category = eventCategoryRepository.findById(dto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy danh mục với ID: " + dto.getCategoryId()));

                Event event = new Event();
                event.setTitle(dto.getTitle());
                event.setDescription(dto.getDescription());
                event.setLocation(dto.getLocation());
                event.setCategory(category);
                event.setStatus(EventStatus.PENDING);
                // Note: createdBy sẽ được set thông qua Security Context hoặc từ controller

                Event savedEvent = eventRepository.save(event);

                // Tạo EventTimes
                if (dto.getEventTimes() != null && !dto.getEventTimes().isEmpty()) {
                        List<EventTime> eventTimes = dto.getEventTimes().stream()
                                        .map(timeDTO -> {
                                                EventTime eventTime = new EventTime();
                                                eventTime.setEvent(savedEvent);
                                                eventTime.setStartTime(timeDTO.getStartTime());
                                                eventTime.setEndTime(timeDTO.getEndTime());
                                                return eventTime;
                                        })
                                        .collect(Collectors.toList());
                        eventTimeRepository.saveAll(eventTimes); // Batch save
                        savedEvent.setEventTimes(eventTimes);
                }

                // Tạo TicketTypes
                if (dto.getTicketTypes() != null && !dto.getTicketTypes().isEmpty()) {
                        List<com.codegym.appticket.entity.TicketType> ticketTypes = dto.getTicketTypes().stream()
                                        .map(ticketTypeDTO -> {
                                                com.codegym.appticket.entity.TicketType ticketType = new com.codegym.appticket.entity.TicketType();
                                                ticketType.setEvent(savedEvent);
                                                ticketType.setName(ticketTypeDTO.getName());
                                                ticketType.setPrice(ticketTypeDTO.getPrice());
                                                ticketType.setQuantity(ticketTypeDTO.getQuantity());
                                                return ticketType;
                                        })
                                        .collect(Collectors.toList());
                        ticketTypeRepository.saveAll(ticketTypes);
                }

                // Handle Media Files (Cloudinary)
                List<EventMedia> eventMedias = new java.util.ArrayList<>();

                // 1. Banner
                // 1. Banner
                if (dto.getBannerUrl() != null && !dto.getBannerUrl().isEmpty()) {
                        eventMedias.add(createMedia(savedEvent, dto.getBannerUrl(),
                                        com.codegym.appticket.entity.MediaType.IMAGE,
                                        com.codegym.appticket.entity.MediaPurpose.BANNER, true));
                }

                // 2. Logo
                if (dto.getLogoUrl() != null && !dto.getLogoUrl().isEmpty()) {
                        eventMedias.add(createMedia(savedEvent, dto.getLogoUrl(),
                                        com.codegym.appticket.entity.MediaType.IMAGE,
                                        com.codegym.appticket.entity.MediaPurpose.LOGO, false));
                }

                // 3. Ticket Map
                if (dto.getTicketMapUrl() != null && !dto.getTicketMapUrl().isEmpty()) {
                        eventMedias.add(createMedia(savedEvent, dto.getTicketMapUrl(),
                                        com.codegym.appticket.entity.MediaType.IMAGE,
                                        com.codegym.appticket.entity.MediaPurpose.TICKET_MAP, false));
                }

                // 4. Gallery
                if (dto.getGalleryUrls() != null && !dto.getGalleryUrls().isEmpty()) {
                        for (String url : dto.getGalleryUrls()) {
                                if (url != null && !url.isEmpty()) {
                                        // Simple logic: if connection ends with .mp4 then video, else image.
                                        // Or keep it simple as IMAGE for now as detailed content type check is harder
                                        // with just URL
                                        com.codegym.appticket.entity.MediaType type = url.endsWith(".mp4")
                                                        || url.endsWith(".webm")
                                                                        ? com.codegym.appticket.entity.MediaType.VIDEO
                                                                        : com.codegym.appticket.entity.MediaType.IMAGE;
                                        eventMedias.add(createMedia(savedEvent, url, type,
                                                        com.codegym.appticket.entity.MediaPurpose.GALLERY, false));
                                }
                        }
                }
                if (!eventMedias.isEmpty()) {
                        eventMediaRepository.saveAll(eventMedias);
                        savedEvent.setEventMedias(eventMedias);
                }

                Event finalEvent = eventRepository.save(savedEvent);
                return convertToDTO(finalEvent);
        }

        private EventMedia createMedia(Event event, String url, com.codegym.appticket.entity.MediaType type,
                        com.codegym.appticket.entity.MediaPurpose purpose, boolean isThumbnail) {
                EventMedia media = new EventMedia();
                media.setEvent(event);
                media.setMediaUrl(url);
                media.setMediaType(type);
                media.setMediaPurpose(purpose);
                media.setIsThumbnail(isThumbnail);
                return media;
        }

        @Override
        @Transactional
        public EventDTO update(Long id, EventUpdateDTO dto) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + id));

                EventCategory category = eventCategoryRepository.findById(dto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy danh mục với ID: " + dto.getCategoryId()));

                event.setTitle(dto.getTitle());
                event.setDescription(dto.getDescription());
                event.setLocation(dto.getLocation());
                event.setCategory(category);
                event.setStatus(dto.getStatus());

                // Cập nhật EventTimes: xóa cũ và tạo mới
                event.getEventTimes().clear();
                if (dto.getEventTimes() != null && !dto.getEventTimes().isEmpty()) {
                        List<EventTime> newEventTimes = dto.getEventTimes().stream()
                                        .map(timeDTO -> {
                                                EventTime eventTime = new EventTime();
                                                eventTime.setEvent(event);
                                                eventTime.setStartTime(timeDTO.getStartTime());
                                                eventTime.setEndTime(timeDTO.getEndTime());
                                                return eventTime;
                                        })
                                        .collect(Collectors.toList());
                        event.getEventTimes().addAll(newEventTimes);
                }

                // Cập nhật TicketTypes: xóa cũ và tạo mới
                ticketTypeRepository.deleteByEventId(event.getId());
                if (dto.getTicketTypes() != null && !dto.getTicketTypes().isEmpty()) {
                        List<com.codegym.appticket.entity.TicketType> ticketTypes = dto.getTicketTypes().stream()
                                        .map(ticketTypeDTO -> {
                                                com.codegym.appticket.entity.TicketType ticketType = new com.codegym.appticket.entity.TicketType();
                                                ticketType.setEvent(event);
                                                ticketType.setName(ticketTypeDTO.getName());
                                                ticketType.setPrice(ticketTypeDTO.getPrice());
                                                ticketType.setQuantity(ticketTypeDTO.getQuantity());
                                                return ticketType;
                                        })
                                        .collect(Collectors.toList());
                        ticketTypeRepository.saveAll(ticketTypes);
                }

                // Cập nhật EventMedias: Logic upload đè
                // 1. Banner
                if (dto.getBannerUrl() != null && !dto.getBannerUrl().isEmpty()) {
                        removeMediaByPurpose(event, com.codegym.appticket.entity.MediaPurpose.BANNER);
                        event.getEventMedias()
                                        .add(createMedia(event, dto.getBannerUrl(),
                                                        com.codegym.appticket.entity.MediaType.IMAGE,
                                                        com.codegym.appticket.entity.MediaPurpose.BANNER, true));
                }

                // 2. Logo
                if (dto.getLogoUrl() != null && !dto.getLogoUrl().isEmpty()) {
                        removeMediaByPurpose(event, com.codegym.appticket.entity.MediaPurpose.LOGO);
                        event.getEventMedias()
                                        .add(createMedia(event, dto.getLogoUrl(),
                                                        com.codegym.appticket.entity.MediaType.IMAGE,
                                                        com.codegym.appticket.entity.MediaPurpose.LOGO, false));
                }

                // 3. Ticket Map
                if (dto.getTicketMapUrl() != null && !dto.getTicketMapUrl().isEmpty()) {
                        removeMediaByPurpose(event, com.codegym.appticket.entity.MediaPurpose.TICKET_MAP);
                        event.getEventMedias()
                                        .add(createMedia(event, dto.getTicketMapUrl(),
                                                        com.codegym.appticket.entity.MediaType.IMAGE,
                                                        com.codegym.appticket.entity.MediaPurpose.TICKET_MAP, false));
                }

                // 4. Gallery (Append mode)
                if (dto.getGalleryUrls() != null && !dto.getGalleryUrls().isEmpty()) {
                        for (String url : dto.getGalleryUrls()) {
                                if (url != null && !url.isEmpty()) {
                                        com.codegym.appticket.entity.MediaType type = url.endsWith(".mp4")
                                                        || url.endsWith(".webm")
                                                                        ? com.codegym.appticket.entity.MediaType.VIDEO
                                                                        : com.codegym.appticket.entity.MediaType.IMAGE;
                                        event.getEventMedias().add(createMedia(event, url, type,
                                                        com.codegym.appticket.entity.MediaPurpose.GALLERY, false));
                                }
                        }
                }

                Event updatedEvent = eventRepository.save(event);
                return convertToDTO(updatedEvent);
        }

        private void removeMediaByPurpose(Event event, com.codegym.appticket.entity.MediaPurpose purpose) {
                event.getEventMedias().removeIf(m -> m.getMediaPurpose() == purpose);
        }

        @Override
        @Transactional
        public void delete(Long id) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + id));
                // Explicitly delete ticket types since they are not cascaded in Entity
                ticketTypeRepository.deleteByEventId(id);
                eventRepository.delete(event);
        }

        private EventDTO convertToDTO(Event event) {
                // Convert EventTimes
                List<EventTimeDTO> eventTimeDTOs = event.getEventTimes().stream()
                                .map(eventTime -> EventTimeDTO.builder()
                                                .id(eventTime.getId())
                                                .startTime(eventTime.getStartTime())
                                                .endTime(eventTime.getEndTime())
                                                .build())
                                .collect(Collectors.toList());

                // Convert EventMedias
                List<EventMediaDTO> eventMediaDTOs = event.getEventMedias().stream()
                                .map(eventMedia -> EventMediaDTO.builder()
                                                .id(eventMedia.getId())
                                                .mediaUrl(eventMedia.getMediaUrl())
                                                .mediaType(eventMedia.getMediaType())
                                                .mediaPurpose(eventMedia.getMediaPurpose())
                                                .isThumbnail(eventMedia.getIsThumbnail())
                                                .build())
                                .collect(Collectors.toList());

                return EventDTO.builder()
                                .id(event.getId())
                                .title(event.getTitle())
                                .description(event.getDescription())
                                .location(event.getLocation())
                                .categoryId(event.getCategory() != null ? event.getCategory().getId() : null)
                                .categoryName(event.getCategory() != null ? event.getCategory().getName() : null)
                                .createdById(event.getCreatedBy() != null ? event.getCreatedBy().getId() : null)
                                .createdByName(event.getCreatedBy() != null ? event.getCreatedBy().getFullName() : null)
                                .status(event.getStatus())
                                .createdAt(event.getCreatedDate())
                                .eventTimes(eventTimeDTOs)
                                .eventMedias(eventMediaDTOs)
                                .ticketTypes(ticketTypeRepository.findByEventId(event.getId()).stream()
                                                .map(tt -> TicketTypeDTO.builder()
                                                                .id(tt.getId())
                                                                .name(tt.getName())
                                                                .price(tt.getPrice())
                                                                .quantity(tt.getQuantity())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();
        }

    @Override
    public List<UpComingEventDTO> findUpComingEvents() {
        return eventRepository.findUpComingEvents();
    }

    @Override
    public List<TrendingEventDTO> findTopTrendingEvents() {
        return eventRepository.findTopTrendingEvents();
    }


}
