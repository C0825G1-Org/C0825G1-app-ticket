package com.codegym.appticket.service.impl;

import com.codegym.appticket.dto.event.EventCreateDTO;
import com.codegym.appticket.dto.event.EventDTO;
import com.codegym.appticket.dto.event.EventMediaDTO;

import com.codegym.appticket.dto.event.EventUpdateDTO;
import com.codegym.appticket.dto.event.TicketTypeDTO;
import com.codegym.appticket.dto.home.HomeEventDTO;
import com.codegym.appticket.dto.home.NearByEventDTO;
import com.codegym.appticket.dto.home.NearByEventWithOccurrencesDTO;
import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.dto.event.EventOccurrenceDTO;
import com.codegym.appticket.dto.event.EventSearchDTO;

import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.EventCategory;
import com.codegym.appticket.entity.EventMedia;
import com.codegym.appticket.entity.EventOccurrence;
import com.codegym.appticket.entity.EventStatus;
import com.codegym.appticket.entity.Location;
import com.codegym.appticket.entity.MediaPurpose;
import com.codegym.appticket.entity.MediaType;
import com.codegym.appticket.entity.Province;
import com.codegym.appticket.entity.User;
import com.codegym.appticket.entity.TicketType;
import com.codegym.appticket.entity.Ward;
import com.codegym.appticket.repository.*;
import com.codegym.appticket.entity.EventCancellationHistory;
import java.time.LocalDateTime;

import com.codegym.appticket.service.IEventService;
import com.codegym.appticket.service.IGeoLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService implements IEventService {

        private final IGeoLocationService geocodingService;
        private final IEventRepository eventRepository;
        private final IEventCategoryRepository eventCategoryRepository;
        private final ILocationRepository locationRepository;
        private final IProvinceRepository provinceRepository;
        private final IWardRepository wardRepository;
        private final IEventMediaRepository eventMediaRepository;
        private final ITicketTypeRepository ticketTypeRepository;
        private final IEventOccurrenceRepository eventOccurrenceRepository;
        private final AdminNotificationService adminNotificationService;
        private final com.codegym.appticket.repository.IUserRepository userRepository;
        private final com.codegym.appticket.util.ProvinceNameMapper provinceNameMapper;
        private final IEventCancellationHistoryRepository eventCancellationHistoryRepository;
        private final com.codegym.appticket.service.IEmailService emailService;

        @Override
        @Transactional(readOnly = true)
        public org.springframework.data.domain.Page<EventDTO> findAll(
                        org.springframework.data.domain.Pageable pageable) {
                return eventRepository.findByStatusNot(EventStatus.DELETED, pageable).map(this::convertToDTO);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<HomeEventDTO> findAllEvent(int page, int size) {
                Sort sort = Sort.by(Sort.Direction.ASC, "id");
                return eventRepository.findAllEvent(PageRequest.of(page, size, sort));
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
                                dto.getStatus(),
                                start,
                                end,
                                pageable).map(this::convertToDTO);
        }

        @Override
        @Transactional(readOnly = true)
        public EventDTO findById(Long id) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + id));
                return convertToDTO(event);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<EventDTO> findByStatus(
                        EventStatus status,
                        Pageable pageable) {
                return eventRepository.findByStatus(status, pageable).map(this::convertToDTO);
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
                event.setCategory(category);

                if (dto.getEventOccurrences() != null && !dto.getEventOccurrences().isEmpty()) {
                        EventOccurrenceDTO firstOcc = dto.getEventOccurrences().get(0);
                        Double[] coords = geocodingService.getCoordinates(firstOcc.getProvinceName());
                        if (coords != null) {
                                event.setLatitude(coords[0]);
                                event.setLongitude(coords[1]);
                        }
                }

                // --- LOGIC: Auto-Approve & Organizer Assignment ---
                // Get current user (Creator)
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();
                String currentEmail = auth.getName();
                if (auth.getPrincipal() instanceof com.codegym.appticket.config.CustomOAuth2User) {
                        currentEmail = ((com.codegym.appticket.config.CustomOAuth2User) auth.getPrincipal()).getEmail();
                } else if (auth.getPrincipal() instanceof com.codegym.appticket.dto.user.UserInfoUserDetails) {
                        currentEmail = ((com.codegym.appticket.dto.user.UserInfoUserDetails) auth.getPrincipal())
                                        .getUsername();
                }
                com.codegym.appticket.entity.User currentUser = userRepository.findByEmailAndNotDeleted(currentEmail);
                event.setCreatedBy(currentUser);

                // Determine Organizer
                if (dto.getOrganizerId() != null) {
                        // Admin assigning specific organizer
                        com.codegym.appticket.entity.User organizer = userRepository.findById(dto.getOrganizerId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Không tìm thấy Organizer với ID: " + dto.getOrganizerId()));
                        event.setOrganizer(organizer);
                } else {
                        // Default: Creator is Organizer
                        event.setOrganizer(currentUser);
                }

                // Determine Status
                boolean isAdminOrStaff = auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("STAFF"));

                if (isAdminOrStaff) {
                        event.setStatus(EventStatus.APPROVED);
                } else {
                        event.setStatus(EventStatus.PENDING);
                }
                // --------------------------------------------------

                // Validate Business Rules
                // Validate Business Rules
                validateBusinessRules(dto.getEventOccurrences(), false);
                // Validation logic needs to change to iterate per occurrence but for now
                // skipping deep validation

                Event savedEvent = eventRepository.save(event);

                // Tạo các lần diễn ra sự kiện + Ticket Types
                if (dto.getEventOccurrences() != null && !dto.getEventOccurrences().isEmpty()) {
                        List<EventOccurrence> occurrences = dto.getEventOccurrences()
                                        .stream()
                                        .map(occDTO -> {
                                                EventOccurrence occurrence = new EventOccurrence();
                                                occurrence.setEvent(savedEvent);
                                                occurrence.setStartTime(occDTO.getStartTime());
                                                occurrence.setEndTime(occDTO.getEndTime());
                                                occurrence.setLocation(getOrCreateLocation(occDTO));

                                                // Create Tickets for this Occurrence
                                                if (occDTO.getTicketTypes() != null) {
                                                        List<TicketType> tickets = occDTO.getTicketTypes().stream()
                                                                        .map(tDto -> {
                                                                                TicketType t = new TicketType();
                                                                                t.setEventOccurrence(occurrence);
                                                                                t.setName(tDto.getName());
                                                                                t.setPrice(tDto.getPrice());
                                                                                t.setQuantity(tDto.getQuantity());
                                                                                return t;
                                                                        }).collect(Collectors.toList());
                                                        occurrence.setTicketTypes(tickets);
                                                }
                                                return occurrence;
                                        })
                                        .collect(Collectors.toList());
                        savedEvent.getEventOccurrences().addAll(occurrences);
                        // Repository save cascading should handle this
                        eventRepository.save(savedEvent);
                }

                // Xử lý file media (Cloudinary)
                List<EventMedia> eventMedias = new java.util.ArrayList<>();

                // 1. Ảnh bìa
                if (dto.getBannerUrl() != null && !dto.getBannerUrl().isEmpty()) {
                        eventMedias.add(createMedia(savedEvent, dto.getBannerUrl(),
                                        MediaType.IMAGE,
                                        MediaPurpose.BANNER, true));
                }

                // 2. Logo
                if (dto.getLogoUrl() != null && !dto.getLogoUrl().isEmpty()) {
                        eventMedias.add(createMedia(savedEvent, dto.getLogoUrl(),
                                        MediaType.IMAGE,
                                        MediaPurpose.LOGO, false));
                }

                // 3. Sơ đồ vé
                if (dto.getTicketMapUrl() != null && !dto.getTicketMapUrl().isEmpty()) {
                        eventMedias.add(createMedia(savedEvent, dto.getTicketMapUrl(),
                                        MediaType.IMAGE,
                                        MediaPurpose.TICKET_MAP, false));
                }

                // 4. Thư viện ảnh
                if (dto.getGalleryUrls() != null && !dto.getGalleryUrls().isEmpty()) {
                        for (String url : dto.getGalleryUrls()) {
                                if (url != null && !url.isEmpty()) {
                                        MediaType type = url.endsWith(".mp4")
                                                        || url.endsWith(".webm")
                                                                        ? MediaType.VIDEO
                                                                        : MediaType.IMAGE;
                                        eventMedias.add(createMedia(savedEvent, url, type,
                                                        MediaPurpose.GALLERY, false));
                                }
                        }
                }
                if (!eventMedias.isEmpty()) {
                        eventMediaRepository.saveAll(eventMedias);
                        savedEvent.getEventMedias().addAll(eventMedias);
                }

                Event finalEvent = eventRepository.save(savedEvent);

                // Thông báo cho Admin
                try {
                        adminNotificationService.sendNotification(finalEvent);
                } catch (Exception e) {
                        System.err.println("Error sending notification: " + e.getMessage());
                }

                return convertToDTO(finalEvent);
        }

        private EventMedia createMedia(Event event, String url, MediaType type,
                        MediaPurpose purpose, boolean isThumbnail) {
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
                                .orElseThrow(() -> new RuntimeException("Event not found"));

                // Validate Business Rules
                // Validate Business Rules
                validateBusinessRules(dto.getEventOccurrences(), true);

                EventCategory category = eventCategoryRepository.findById(dto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy danh mục với ID: " + dto.getCategoryId()));

                event.setTitle(dto.getTitle());
                event.setDescription(dto.getDescription());
                event.setCategory(category);
                event.setStatus(dto.getStatus());

                if (dto.getEventOccurrences() != null && !dto.getEventOccurrences().isEmpty()) {
                        EventOccurrenceDTO firstOcc = dto.getEventOccurrences().get(0);
                        Double[] coords = geocodingService.getCoordinates(firstOcc.getProvinceName());
                        if (coords != null) {
                                event.setLatitude(coords[0]);
                                event.setLongitude(coords[1]);
                        }
                }

                // --- LOGIC: Update Rules ---
                // 1. Organizer Update (Admin only typically, or if allowed)
                if (dto.getOrganizerId() != null) {
                        com.codegym.appticket.entity.User organizer = userRepository.findById(dto.getOrganizerId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Không tìm thấy Organizer với ID: " + dto.getOrganizerId()));
                        event.setOrganizer(organizer);
                }

                // 2. Edit Restriction: If APPROVED, cannot edit critical info < 48 hours before
                // start
                if (event.getStatus() == EventStatus.APPROVED) {
                        LocalDateTime earliestStart = event.getEventOccurrences().stream()
                                        .map(EventOccurrence::getStartTime)
                                        .min(LocalDateTime::compareTo)
                                        .orElse(null);

                        if (earliestStart != null) {
                                long hoursUntilStart = java.time.Duration.between(LocalDateTime.now(), earliestStart)
                                                .toHours();
                                if (hoursUntilStart < 48) {
                                        throw new RuntimeException(
                                                        "Không thể chỉnh sửa sự kiện đã duyệt trong vòng 48 giờ trước khi bắt đầu.");
                                }
                        }
                }

                // Cập nhật các lần diễn ra sự kiện: Merge logic
                List<EventOccurrenceDTO> incomingOccurrences = dto.getEventOccurrences() != null
                                ? dto.getEventOccurrences()
                                : new ArrayList<>();
                List<EventOccurrence> currentOccurrences = event.getEventOccurrences();

                // 1. Xóa các occurrence không còn trong list mới
                List<Long> incomingOccurrenceIds = incomingOccurrences.stream()
                                .map(EventOccurrenceDTO::getId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                currentOccurrences.removeIf(occ -> !incomingOccurrenceIds.contains(occ.getId()));

                // 2. Cập nhật hoặc Thêm mới
                for (EventOccurrenceDTO occDTO : incomingOccurrences) {
                        EventOccurrence target = null;
                        if (occDTO.getId() != null) {
                                target = currentOccurrences.stream()
                                                .filter(o -> o.getId().equals(occDTO.getId()))
                                                .findFirst()
                                                .orElse(null);
                        }

                        boolean isNew = false;
                        if (target == null) {
                                target = new EventOccurrence();
                                target.setEvent(event);
                                isNew = true;
                        }

                        target.setStartTime(occDTO.getStartTime());
                        target.setEndTime(occDTO.getEndTime());
                        target.setLocation(getOrCreateLocation(occDTO));

                        // Update Tickets specific to this occurrence
                        if (occDTO.getTicketTypes() != null) {
                                List<TicketTypeDTO> incomingTickets = occDTO.getTicketTypes();
                                List<TicketType> currentTickets = target.getTicketTypes();

                                // Delete tickets not in incoming
                                if (currentTickets == null) {
                                        currentTickets = new ArrayList<>();
                                        target.setTicketTypes(currentTickets);
                                }

                                List<Long> incomingTicketIds = incomingTickets.stream()
                                                .map(TicketTypeDTO::getId)
                                                .filter(Objects::nonNull)
                                                .collect(Collectors.toList());

                                currentTickets.removeIf(t -> !incomingTicketIds.contains(t.getId()));

                                // Update/Add
                                for (TicketTypeDTO tDTO : incomingTickets) {
                                        TicketType tTarget = null;
                                        if (tDTO.getId() != null) {
                                                tTarget = currentTickets.stream()
                                                                .filter(t -> t.getId().equals(tDTO.getId()))
                                                                .findFirst().orElse(null);
                                        }

                                        if (tTarget == null) {
                                                tTarget = new TicketType();
                                                tTarget.setEventOccurrence(target);
                                                currentTickets.add(tTarget);
                                        }
                                        tTarget.setName(tDTO.getName());
                                        tTarget.setPrice(tDTO.getPrice());
                                        tTarget.setQuantity(tDTO.getQuantity());
                                }
                        }

                        if (isNew) {
                                currentOccurrences.add(target);
                        }
                }

                // Cập nhật media sự kiện: Logic tải lên ghi đè
                // 1. Ảnh bìa
                if (dto.getBannerUrl() != null && !dto.getBannerUrl().isEmpty()) {
                        removeMediaByPurpose(event, MediaPurpose.BANNER);
                        event.getEventMedias()
                                        .add(createMedia(event, dto.getBannerUrl(),
                                                        MediaType.IMAGE,
                                                        MediaPurpose.BANNER, true));
                }

                // 2. Logo
                if (dto.getLogoUrl() != null && !dto.getLogoUrl().isEmpty()) {
                        removeMediaByPurpose(event, MediaPurpose.LOGO);
                        event.getEventMedias()
                                        .add(createMedia(event, dto.getLogoUrl(),
                                                        MediaType.IMAGE,
                                                        MediaPurpose.LOGO, false));
                }

                // 3. Sơ đồ vé
                if (dto.getTicketMapUrl() != null && !dto.getTicketMapUrl().isEmpty()) {
                        removeMediaByPurpose(event, MediaPurpose.TICKET_MAP);
                        event.getEventMedias()
                                        .add(createMedia(event, dto.getTicketMapUrl(),
                                                        MediaType.IMAGE,
                                                        MediaPurpose.TICKET_MAP, false));
                }

                // 4. Thư viện ảnh (Chế độ thay thế)
                removeMediaByPurpose(event, MediaPurpose.GALLERY);
                if (dto.getGalleryUrls() != null && !dto.getGalleryUrls().isEmpty()) {
                        for (String url : dto.getGalleryUrls()) {
                                if (url != null && !url.isEmpty()) {
                                        MediaType type = url.endsWith(".mp4")
                                                        || url.endsWith(".webm")
                                                                        ? MediaType.VIDEO
                                                                        : MediaType.IMAGE;
                                        event.getEventMedias().add(createMedia(event, url, type,
                                                        MediaPurpose.GALLERY, false));
                                }
                        }
                }

                Event updatedEvent = eventRepository.save(event);
                return convertToDTO(updatedEvent);
        }

        private void removeMediaByPurpose(Event event, MediaPurpose purpose) {
                event.getEventMedias().removeIf(m -> m.getMediaPurpose() == purpose);
        }

        @Override
        @Transactional
        public void delete(Long id) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + id));
                // Chuyển sang Soft Delete để tránh lỗi khóa ngoại và giữ lịch sử
                event.setStatus(EventStatus.DELETED);
                eventRepository.save(event);
        }

        private EventDTO convertToDTO(Event event) {
                // Chuyển đổi các lần diễn ra sự kiện
                List<EventOccurrenceDTO> occurrenceDTOs = event.getEventOccurrences()
                                .stream()
                                .map(occ -> {
                                        String provinceName = occ.getLocation().getWard().getProvince().getName();
                                        String wardName = occ.getLocation().getWard().getName();

                                        // Map Tickets nested in Occurrence
                                        List<TicketTypeDTO> ticketDTOs = new ArrayList<>();
                                        if (occ.getTicketTypes() != null) {
                                                ticketDTOs = occ.getTicketTypes().stream()
                                                                .map(tt -> TicketTypeDTO.builder()
                                                                                .id(tt.getId())
                                                                                .name(tt.getName())
                                                                                .price(tt.getPrice())
                                                                                .quantity(tt.getQuantity())
                                                                                .build())
                                                                .collect(Collectors.toList());
                                        }

                                        return EventOccurrenceDTO.builder()
                                                        .id(occ.getId())
                                                        .startTime(occ.getStartTime())
                                                        .endTime(occ.getEndTime())
                                                        .provinceCode(occ.getLocation().getWard().getProvince()
                                                                        .getCode())
                                                        .provinceName(provinceName)
                                                        .wardCode(occ.getLocation().getWard().getCode())
                                                        .wardName(wardName)
                                                        .addressDetail(occ.getLocation().getAddressDetail())
                                                        .mapLink(occ.getLocation().getMapLink())
                                                        .ticketTypes(ticketDTOs)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // Chuyển đổi media sự kiện
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
                                .categoryId(event.getCategory() != null ? event.getCategory().getId() : null)
                                .categoryName(event.getCategory() != null ? event.getCategory().getName() : null)
                                .createdById(event.getCreatedBy() != null ? event.getCreatedBy().getId() : null)
                                .createdByName(event.getCreatedBy() != null ? event.getCreatedBy().getFullName() : null)
                                .status(event.getStatus())
                                .createdAt(event.getCreatedDate())
                                // Map Organizer Info
                                .organizerId(event.getOrganizer() != null ? event.getOrganizer().getId() : null)
                                .organizerName(event.getOrganizer() != null ? event.getOrganizer().getFullName() : null)
                                .reason(event.getCancellationHistories() != null
                                                && !event.getCancellationHistories().isEmpty()
                                                                ? event.getCancellationHistories().stream()
                                                                                .max(java.util.Comparator.comparing(
                                                                                                com.codegym.appticket.entity.EventCancellationHistory::getCreatedDate))
                                                                                .map(com.codegym.appticket.entity.EventCancellationHistory::getReason)
                                                                                .orElse(null)
                                                                : null)

                                .eventOccurrences(occurrenceDTOs)
                                .eventMedias(eventMediaDTOs)
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

        @Override
        public Page<HomeEventDTO> searchHomeEvents(String searchText, Long categoryId, String location, int page,
                        int size, String sort) {
                // Sắp xếp trong Java sử dụng Pageable
                Sort sortOrder = Sort.by(Sort.Direction.ASC, "id");
                Pageable pageable = PageRequest.of(page, size, sortOrder);

                // Convert location string to list of variants
                List<String> locationVariants = new ArrayList<>();
                int hasLocationFilter = 0; // 0 = false, 1 = true (MySQL compatible)

                if (location != null && !location.trim().isEmpty()) {
                        locationVariants = provinceNameMapper.getProvinceVariants(location);
                        hasLocationFilter = locationVariants.isEmpty() ? 0 : 1;
                }

                return eventRepository.searchHomeEvents(searchText, categoryId, locationVariants, hasLocationFilter,
                                pageable);
        }

        private Location getOrCreateLocation(EventOccurrenceDTO occDTO) {
                // 1. Xử lý Tỉnh/Thành phố
                Province province = provinceRepository
                                .findById(occDTO.getProvinceCode())
                                .orElseGet(() -> {
                                        Province newProv = new Province();
                                        newProv.setCode(occDTO.getProvinceCode());
                                        newProv.setName(occDTO.getProvinceName());
                                        return provinceRepository.save(newProv);
                                });

                // 2. Xử lý Phường/Xã
                Ward ward = wardRepository
                                .findById(occDTO.getWardCode())
                                .orElseGet(() -> {
                                        Ward newWard = new Ward();
                                        newWard.setCode(occDTO.getWardCode());
                                        newWard.setName(occDTO.getWardName());
                                        newWard.setProvince(province);
                                        return wardRepository.save(newWard);
                                });

                return locationRepository
                                .findByWardCodeAndAddressDetail(
                                                occDTO.getWardCode(),
                                                occDTO.getAddressDetail())
                                .orElseGet(() -> {
                                        Location newLoc = new Location();
                                        newLoc.setWard(ward);
                                        newLoc.setAddressDetail(
                                                        occDTO.getAddressDetail());
                                        newLoc.setMapLink(occDTO.getMapLink());
                                        return locationRepository.saveAndFlush(newLoc);
                                });
        }

        @Override
        public Page<Event> findEventsByOrganizer(User organizer, Pageable pageable) {
                return eventRepository.findByOrganizerAndStatusNot(organizer, EventStatus.DELETED, pageable);
        }

        @Override
        @Transactional
        public void approve(Long id) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));
                event.setStatus(EventStatus.APPROVED);
                eventRepository.save(event);

                // Send notification email to organizer
                if (emailService != null) {
                        try {
                                emailService.sendEventApprovalNotification(event);
                        } catch (Exception e) {
                                // Log but don't fail the transaction
                                System.err.println("Failed to send approval email: " + e.getMessage());
                        }
                }
        }

        @Override
        @Transactional
        public void reject(Long id, String reason) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));
                event.setStatus(EventStatus.REJECTED);
                eventRepository.save(event);

                saveCancellationHistory(event, EventStatus.REJECTED, reason);

                // Send notification email
                if (emailService != null) {
                        try {
                                emailService.sendEventRejectionNotification(event, reason);
                        } catch (Exception e) {
                                System.err.println("Failed to send rejection email: " + e.getMessage());
                        }
                }
        }

        @Override
        public List<NearByEventDTO> findNearbyEvents(Double userLatitude, Double userLongitude, String excludeLocation,
                        int limit) {
                // Convert exclude location to variants list
                List<String> excludeLocationVariants = new ArrayList<>();
                int hasExcludeFilter = 0;

                if (excludeLocation != null && !excludeLocation.trim().isEmpty()) {
                        excludeLocationVariants = provinceNameMapper.getProvinceVariants(excludeLocation);
                        hasExcludeFilter = excludeLocationVariants.isEmpty() ? 0 : 1;
                }

                return eventRepository.findNearbyEvents(userLatitude, userLongitude, excludeLocationVariants,
                                hasExcludeFilter, limit);
        }

        @Override
        public List<NearByEventWithOccurrencesDTO> findNearbyEventsGrouped(Double userLatitude, Double userLongitude,
                        String excludeLocation, int limit) {
                // Convert exclude location to variants list
                List<String> excludeLocationVariants = new ArrayList<>();
                int hasExcludeFilter = 0;

                if (excludeLocation != null && !excludeLocation.trim().isEmpty()) {
                        excludeLocationVariants = provinceNameMapper.getProvinceVariants(excludeLocation);
                        hasExcludeFilter = excludeLocationVariants.isEmpty() ? 0 : 1;
                }

                // Get all nearby event occurrences from repository
                List<NearByEventDTO> allOccurrences = eventRepository.findNearbyEvents(userLatitude, userLongitude,
                                excludeLocationVariants, hasExcludeFilter, limit * 3);

                // Group occurrences by event ID using LinkedHashMap to preserve order
                Map<Long, NearByEventWithOccurrencesDTO> eventMap = new LinkedHashMap<>();

                for (NearByEventDTO occurrence : allOccurrences) {
                        Long eventId = occurrence.getId();

                        // Get or create the grouped event DTO
                        NearByEventWithOccurrencesDTO groupedEvent = eventMap.get(eventId);

                        if (groupedEvent == null) {
                                // First occurrence for this event - create new grouped DTO
                                groupedEvent = new NearByEventWithOccurrencesDTO();
                                groupedEvent.setId(eventId);
                                groupedEvent.setTitle(occurrence.getTitle());
                                groupedEvent.setLocation(occurrence.getLocation());
                                groupedEvent.setImage(occurrence.getImage());
                                groupedEvent.setCategoryName(occurrence.getCategoryName());
                                groupedEvent.setDistance(occurrence.getDistance()); // Distance to nearest occurrence
                                groupedEvent.setOccurrences(new ArrayList<>());

                                eventMap.put(eventId, groupedEvent);
                        }

                        // Add this occurrence to the event's occurrence list
                        NearByEventWithOccurrencesDTO.OccurrenceInfo occInfo = new NearByEventWithOccurrencesDTO.OccurrenceInfo();
                        occInfo.setOccurrenceId(occurrence.getOccurrenceId());
                        occInfo.setEventDate(occurrence.getEventDate());
                        occInfo.setAddressDetail(occurrence.getAddressDetail());
                        occInfo.setDistance(occurrence.getDistance());

                        groupedEvent.getOccurrences().add(occInfo);
                }

                // Convert map values to list and limit to requested number of unique events
                return eventMap.values().stream()
                                .limit(limit)
                                .collect(Collectors.toList());
        }

        @Override
        public long countByStatus(EventStatus status) {
                return eventRepository.countByStatus(status);
        }

        @Override
        public long countByStatuses(List<EventStatus> statuses) {
                return eventRepository.countByStatusIn(statuses);
        }

        @Override
        public long countAll() {
                return eventRepository.count();
        }

        private void validateBusinessRules(List<EventOccurrenceDTO> occurrences, boolean isUpdate) {
                if (occurrences == null || occurrences.isEmpty()) {
                        throw new RuntimeException("Sự kiện cần có ít nhất một lịch trình.");
                }

                LocalDateTime now = LocalDateTime.now();
                // Rule: Organization needs at least 3 days for ticket sales & approval
                LocalDateTime minStartTime = now.plusDays(3);

                for (EventOccurrenceDTO occ : occurrences) {
                        // 1. Time Validation
                        if (occ.getStartTime() == null || occ.getEndTime() == null) {
                                throw new RuntimeException("Thời gian bắt đầu và kết thúc không được để trống.");
                        }

                        // Duration >= 30 minutes
                        if (occ.getEndTime().isBefore(occ.getStartTime().plusMinutes(30))) {
                                throw new RuntimeException("Thời gian diễn ra sự kiện phải từ 30 phút trở lên.");
                        }

                        // Lead Time > 3 days (Apply strict check for NEW occurrences)
                        // If Create (isUpdate=false) -> Check all
                        // If Update (isUpdate=true) -> Check only if it's a new occurrence (id == null)
                        boolean shouldCheckLeadTime = !isUpdate || (occ.getId() == null);
                        if (shouldCheckLeadTime && occ.getStartTime().isBefore(minStartTime)) {
                                throw new RuntimeException(
                                                "Lịch trình phải được tạo trước thời gian diễn ra ít nhất 3 ngày để đảm bảo quy trình duyệt và bán vé.");
                        }

                        // 2. Ticket Validation
                        if (occ.getTicketTypes() != null) {
                                for (TicketTypeDTO t : occ.getTicketTypes()) {
                                        if (t.getPrice() != null
                                                        && t.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
                                                throw new RuntimeException("Giá vé không được âm.");
                                        }
                                        if (t.getQuantity() != null && t.getQuantity() <= 0) {
                                                throw new RuntimeException("Số lượng vé phải lớn hơn 0.");
                                        }
                                }
                        }
                }
        }

        @Override
        @Transactional
        public void cancel(Long id, String reason) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));
                // Optional: Check if event can be cancelled (e.g. not already
                // cancelled/deleted)
                if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.DELETED) {
                        throw new RuntimeException("Sự kiện đã bị hủy hoặc xóa trước đó.");
                }

                event.setStatus(EventStatus.CANCELLED);
                eventRepository.save(event);

                saveCancellationHistory(event, EventStatus.CANCELLED, reason);

                // Send notification email
                if (emailService != null) {
                        try {
                                emailService.sendEventCancellationNotification(event, reason);
                        } catch (Exception e) {
                                System.err.println("Failed to send cancellation email: " + e.getMessage());
                        }
                }
        }

        @Override
        @Transactional
        public void restore(Long eventId) {
                Event event = eventRepository.findById(eventId)
                                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại!"));

                // Reset status to PENDING
                event.setStatus(EventStatus.PENDING);
                eventRepository.save(event);
        }

        private void saveCancellationHistory(Event event, EventStatus status, String reason) {
                User currentUser = getCurrentUser();
                EventCancellationHistory history = new EventCancellationHistory();
                history.setEvent(event);
                history.setUser(currentUser);
                history.setStatus(status);
                history.setReason(reason);
                eventCancellationHistoryRepository.save(history);
        }

        private User getCurrentUser() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                        return null;
                }

                String currentEmail = auth.getName();
                if (auth.getPrincipal() instanceof com.codegym.appticket.config.CustomOAuth2User) {
                        currentEmail = ((com.codegym.appticket.config.CustomOAuth2User) auth.getPrincipal()).getEmail();
                } else if (auth.getPrincipal() instanceof com.codegym.appticket.dto.user.UserInfoUserDetails) {
                        currentEmail = ((com.codegym.appticket.dto.user.UserInfoUserDetails) auth.getPrincipal())
                                        .getUsername();
                }
                return userRepository.findByEmailAndNotDeleted(currentEmail);
        }
}
