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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;

@Service
@RequiredArgsConstructor
@Slf4j
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
        private final jakarta.persistence.EntityManager entityManager;
        private final com.codegym.appticket.repository.IBookingDetailRepository bookingDetailRepository;

        @Override
        @Transactional(readOnly = true)
        public org.springframework.data.domain.Page<EventDTO> findAll(
                        org.springframework.data.domain.Pageable pageable) {
                // Enforce custom sort by stripping sort from pageable
                Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
                return eventRepository.findAllWithCustomSort(sortedPageable).map(this::convertToDTO);
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

                // Strip sort to use custom query sort
                Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
                return eventRepository.searchEvents(
                                dto.getTitle(),
                                dto.getCategoryId(),
                                dto.getStatus(),
                                start,
                                end,
                                sortedPageable).map(this::convertToDTO);
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

                if (dto.getStatus() == EventStatus.DRAFT) {
                        event.setStatus(EventStatus.DRAFT);
                } else if (isAdminOrStaff) {
                        event.setStatus(EventStatus.APPROVED);
                } else {
                        event.setStatus(EventStatus.PENDING);
                }
                // --------------------------------------------------

                // Validate Business Rules
                if (event.getStatus() != EventStatus.DRAFT) {
                        validateBusinessRules(dto.getEventOccurrences(), false);
                }
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
                validateBusinessRules(dto.getEventOccurrences(), true);

                EventCategory category = eventCategoryRepository.findById(dto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy danh mục với ID: " + dto.getCategoryId()));

                event.setTitle(dto.getTitle());
                event.setDescription(dto.getDescription());
                event.setCategory(category);
                if (dto.getStatus() != null) {
                        event.setStatus(dto.getStatus());
                }

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
                Set<Location> potentialOrphanLocations = new HashSet<>();
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

                        // Track old location before updating (for cleanup later)
                        if (!isNew && target.getLocation() != null) {
                                potentialOrphanLocations.add(target.getLocation());
                        }

                        target.setStartTime(occDTO.getStartTime());
                        target.setEndTime(occDTO.getEndTime());
                        Location newLocation = getOrCreateLocation(occDTO);
                        target.setLocation(newLocation);
                        // Remove new location from orphan set (in case it's being reused)
                        potentialOrphanLocations.remove(newLocation);

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

                                        // VALIDATION: Prevent reducing quantity below sold quantity
                                        if (tTarget.getId() != null) {
                                                Long soldCount = bookingDetailRepository
                                                                .countSoldTicketsByTicketTypeId(tTarget.getId());
                                                if (tDTO.getQuantity() < soldCount) {
                                                        throw new IllegalArgumentException(
                                                                        "Không thể giảm số lượng vé '" + tDTO.getName()
                                                                                        + "' xuống "
                                                                                        + tDTO.getQuantity()
                                                                                        + " vì đã bán " + soldCount
                                                                                        + " vé.");
                                                }
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
                // Cleanup orphaned locations before flushing
                cleanupOrphanLocations(potentialOrphanLocations);
                // Flush changes to DB and clear persistence context to avoid stale data
                entityManager.flush();
                entityManager.clear();

                // Re-fetch event to ensure all associations are fresh from DB
                Event refreshedEvent = eventRepository.findById(updatedEvent.getId())
                        .orElseThrow(() -> new RuntimeException("Event not found after update"));

                return convertToDTO(refreshedEvent);
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
                                .viewCount(event.getViewCount())

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

            // 3. Tìm hoặc tạo Location
            Location location = locationRepository
                    .findByWardCodeAndAddressDetail(occDTO.getWardCode(), occDTO.getAddressDetail())
                    .orElseGet(() -> {
                            Location newLoc = new Location();
                            newLoc.setWard(ward);
                            newLoc.setAddressDetail(occDTO.getAddressDetail());
                            newLoc.setMapLink(occDTO.getMapLink());
                            return newLoc; // CHƯA save tại đây
                    });

            // 4. Populate coordinates nếu chưa có
            if (location.getLatitude() == null || location.getLongitude() == null) {
                    try {
                        String provinceName = ward.getProvince().getName();
                        System.out.println("📍 Attempting to geocode for province: " + provinceName + " (ward: " + ward.getCode() + ")");

                        Double[] coords = geocodingService.getCoordinates(provinceName);

                        if (coords != null && coords.length == 2) {
                                location.setLatitude(coords[0]);
                                location.setLongitude(coords[1]);
                                System.out.println("✅ Successfully set coordinates: lat=" + coords[0] + ", lon=" + coords[1]);
                        } else {
                                System.err.println("⚠️ Geocoding returned null or invalid coordinates for province: " + provinceName);
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error populating coordinates for ward " + ward.getCode() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
            }

            // 5. Save và trả về (cho cả location mới và cũ nếu cập nhật lat/lon)
            return locationRepository.saveAndFlush(location);
        }

        private void cleanupOrphanLocations(Set<Location> potentialOrphans) {
                if (potentialOrphans == null || potentialOrphans.isEmpty()) {
                        return;
                }

                int deletedCount = 0;
                for (Location location : potentialOrphans) {
                        // Check if this location is still being used by any occurrence
                        long referenceCount = eventOccurrenceRepository.countByLocation(location);

                        if (referenceCount == 0) {
                                // No occurrences reference this location anymore - safe to delete
                                locationRepository.delete(location);
                                deletedCount++;
                                System.out.println("🗑️ Deleted orphan location ID: " + location.getId() +
                                                " (ward: " + location.getWard().getCode() + ", address: " + location.getAddressDetail() + ")");
                        }
                }

                if (deletedCount > 0) {
                        System.out.println("✅ Cleanup completed: " + deletedCount + " orphan location(s) deleted");
                }
        }

        @Override
        public org.springframework.data.domain.Page<Event> findEventsByOrganizer(
                        com.codegym.appticket.entity.User organizer,
                        Pageable pageable) {
                // Enforce custom sort by stripping sort from pageable
                Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
                return eventRepository.findByOrganizerWithCustomSort(organizer.getId(), EventStatus.DELETED,
                                sortedPageable);
        }

        @Override
        @Transactional
        public void approve(Long id) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));

                // VALIDATION: Cannot approve if data is missing
                if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
                        throw new IllegalArgumentException("Tiêu đề sự kiện không được để trống.");
                }
                if (event.getDescription() == null || event.getDescription().trim().isEmpty()) {
                        throw new IllegalArgumentException("Mô tả sự kiện không được để trống.");
                }
                if (event.getCategory() == null) {
                        throw new IllegalArgumentException("Sự kiện chưa có danh mục.");
                }

                // Check Banner
                boolean hasBanner = event.getEventMedias().stream()
                                .anyMatch(m -> m.getMediaPurpose() == MediaPurpose.BANNER);
                if (!hasBanner) {
                        throw new IllegalArgumentException("Sự kiện cần có ảnh bìa (Banner) để công khai.");
                }

                // Check Occurrences
                if (event.getEventOccurrences() == null || event.getEventOccurrences().isEmpty()) {
                        throw new IllegalArgumentException("Sự kiện cần có ít nhất một lịch trình.");
                }

                // Map Entity validation to DTO validation
                List<EventOccurrenceDTO> occDTOs = event.getEventOccurrences().stream()
                                .map(occ -> {
                                        EventOccurrenceDTO dto = new EventOccurrenceDTO();
                                        dto.setId(occ.getId());
                                        dto.setStartTime(occ.getStartTime());
                                        dto.setEndTime(occ.getEndTime());
                                        // Map Tickets
                                        if (occ.getTicketTypes() != null) {
                                                List<TicketTypeDTO> tDTOs = occ.getTicketTypes().stream()
                                                                .map(t -> {
                                                                        TicketTypeDTO tDto = new TicketTypeDTO();
                                                                        tDto.setId(t.getId());
                                                                        tDto.setPrice(t.getPrice());
                                                                        tDto.setQuantity(t.getQuantity());
                                                                        return tDto;
                                                                }).collect(Collectors.toList());
                                                dto.setTicketTypes(tDTOs);
                                        }
                                        return dto;
                                }).collect(Collectors.toList());

                // Run business rules (Treat as "Update" but still enforce strict checks if
                // rules
                // dictate)
                // Note: isUpdate=true allows bypassing "Lead Time > 3 days" for *existing*
                // occurrences.
                // If we want to strictly enforce 3-day rule on Publish, we might need to pass
                // false?
                // However, if I drafted it a week ago for next month, updating it now is fine.
                // If I draft it today for tomorrow, and try to publish today -> should fail.
                // Logic: validateBusinessRules checks (isUpdate || id==null)
                // If id exists (it does for saved draft), isUpdate=true -> skips lead time
                // check
                // for that item.
                // This might be a loophole for "Draft today for tomorrow" -> Save -> Publish.
                // FIX: When "Publishing" a draft, we should treat it as a "New Launch"
                // regarding
                // lead time?
                // Or maybe the 3-day rule is only for "Newly created slots".
                // Let's stick to safe side: Pass `false` (isUpdate=false) to force checking
                // dates
                // against NOW + 3 days?
                // NO, if I created draft 1 month ago for an event in 3 months. Now I publish (2
                // months left).
                // minTime = now + 3 days. StartTime = now + 60 days. Safe.
                // If I created draft today for tomorrow. Publish today.
                // minTime = tomorrow + 2 days. StartTime = tomorrow. Fail. Correct.
                // So passing `false` effectively treats all occurrences as "New" for validation
                // purposes.
                validateBusinessRules(occDTOs, false);

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
        public void submitForApproval(Long id) {
                Event event = eventRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại"));

                // Only allow if Draft
                if (event.getStatus() != EventStatus.DRAFT) {
                        throw new IllegalArgumentException("Chỉ có thể gửi duyệt sự kiện đang ở trạng thái Nháp.");
                }

                // Validation (Strict)
                if (event.getTitle() == null || event.getTitle().trim().isEmpty())
                        throw new IllegalArgumentException("Tiêu đề không được để trống.");
                if (event.getDescription() == null || event.getDescription().trim().isEmpty())
                        throw new IllegalArgumentException("Mô tả không được để trống.");
                if (event.getCategory() == null)
                        throw new IllegalArgumentException("Sự kiện cần có danh mục.");

                boolean hasBanner = event.getEventMedias().stream()
                                .anyMatch(m -> m.getMediaPurpose() == MediaPurpose.BANNER);
                if (!hasBanner)
                        throw new IllegalArgumentException("Sự kiện cần có ảnh bìa (Banner).");

                // Check Occurrences
                if (event.getEventOccurrences() == null || event.getEventOccurrences().isEmpty()) {
                        throw new IllegalArgumentException("Sự kiện cần có ít nhất một lịch trình.");
                }

                // Map Entity validation to DTO validation
                List<EventOccurrenceDTO> occDTOs = event.getEventOccurrences().stream()
                                .map(occ -> {
                                        EventOccurrenceDTO dto = new EventOccurrenceDTO();
                                        dto.setId(occ.getId());
                                        dto.setStartTime(occ.getStartTime());
                                        dto.setEndTime(occ.getEndTime());
                                        // Map Tickets
                                        if (occ.getTicketTypes() != null) {
                                                List<TicketTypeDTO> tDTOs = occ.getTicketTypes().stream()
                                                                .map(t -> {
                                                                        TicketTypeDTO tDto = new TicketTypeDTO();
                                                                        tDto.setId(t.getId());
                                                                        tDto.setPrice(t.getPrice());
                                                                        tDto.setQuantity(t.getQuantity());
                                                                        return tDto;
                                                                }).collect(Collectors.toList());
                                                dto.setTicketTypes(tDTOs);
                                        }
                                        return dto;
                                }).collect(Collectors.toList());

                validateBusinessRules(occDTOs, false);

                event.setStatus(EventStatus.PENDING);
                eventRepository.save(event);
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
                        throw new IllegalArgumentException("Sự kiện cần có ít nhất một lịch trình.");
                }

                LocalDateTime now = LocalDateTime.now();
                // Rule: Organization needs at least 3 days for ticket sales & approval
                LocalDateTime minStartTime = now.plusDays(3);

                for (EventOccurrenceDTO occ : occurrences) {
                        // 1. Time Validation
                        if (occ.getStartTime() == null || occ.getEndTime() == null) {
                                throw new IllegalArgumentException(
                                                "Thời gian bắt đầu và kết thúc không được để trống.");
                        }

                        // Duration >= 30 minutes
                        if (occ.getEndTime().isBefore(occ.getStartTime().plusMinutes(30))) {
                                throw new IllegalArgumentException(
                                                "Thời gian diễn ra sự kiện phải từ 30 phút trở lên.");
                        }

                        // Lead Time > 3 days (Apply strict check for NEW occurrences)
                        // If Create (isUpdate=false) -> Check all
                        // If Update (isUpdate=true) -> Check only if it's a new occurrence (id == null)
                        boolean shouldCheckLeadTime = !isUpdate || (occ.getId() == null);
                        if (shouldCheckLeadTime && occ.getStartTime().isBefore(minStartTime)) {
                                throw new IllegalArgumentException(
                                                "Lịch trình phải được tạo trước thời gian diễn ra ít nhất 3 ngày để đảm bảo quy trình duyệt và bán vé.");
                        }

                        // 2. Ticket Validation
                        if (occ.getTicketTypes() != null) {
                                for (TicketTypeDTO t : occ.getTicketTypes()) {
                                        if (t.getPrice() != null
                                                        && t.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
                                                throw new IllegalArgumentException("Giá vé không được âm.");
                                        }
                                        if (t.getQuantity() != null && t.getQuantity() <= 0) {
                                                throw new IllegalArgumentException("Số lượng vé phải lớn hơn 0.");
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
        public EventDTO duplicate(Long originalId) {
                Event original = eventRepository.findById(originalId)
                                .orElseThrow(() -> new RuntimeException("Original event not found"));

                Event newEvent = new Event();
                newEvent.setTitle(original.getTitle() + " (Sao chép)");
                newEvent.setDescription(original.getDescription());
                newEvent.setCategory(original.getCategory());
                newEvent.setStatus(EventStatus.DRAFT);

                // Set Creator as Current User
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String currentEmail = auth.getName();
                if (auth.getPrincipal() instanceof com.codegym.appticket.config.CustomOAuth2User) {
                        currentEmail = ((com.codegym.appticket.config.CustomOAuth2User) auth.getPrincipal()).getEmail();
                } else if (auth.getPrincipal() instanceof com.codegym.appticket.dto.user.UserInfoUserDetails) {
                        currentEmail = ((com.codegym.appticket.dto.user.UserInfoUserDetails) auth.getPrincipal())
                                        .getUsername();
                }
                User currentUser = userRepository.findByEmailAndNotDeleted(currentEmail);
                newEvent.setCreatedBy(currentUser);
                newEvent.setOrganizer(currentUser); // Default to creator or keep original organizer? Usually creator
                                                    // becomes organizer of copy.

                // Copy Media
                if (original.getEventMedias() != null) {
                        List<EventMedia> newMedias = new ArrayList<>();
                        for (EventMedia m : original.getEventMedias()) {
                                EventMedia nm = new EventMedia();
                                nm.setEvent(newEvent);
                                nm.setMediaUrl(m.getMediaUrl());
                                nm.setMediaType(m.getMediaType());
                                nm.setMediaPurpose(m.getMediaPurpose());
                                nm.setIsThumbnail(m.getIsThumbnail());
                                newMedias.add(nm);
                        }
                        newEvent.setEventMedias(newMedias);
                }

                // Save basic event first to get ID (though Cascade might handle it, safer to
                // init lists)
                if (newEvent.getEventOccurrences() == null)
                        newEvent.setEventOccurrences(new ArrayList<>());

                // Copy Occurrences
                if (original.getEventOccurrences() != null) {
                        for (EventOccurrence occ : original.getEventOccurrences()) {
                                EventOccurrence nOcc = new EventOccurrence();
                                nOcc.setEvent(newEvent);
                                // Shift time by 1 day to make it distinct and practical for "next show"
                                nOcc.setStartTime(occ.getStartTime().plusDays(1));
                                nOcc.setEndTime(occ.getEndTime().plusDays(1));
                                nOcc.setLocation(occ.getLocation());

                                List<TicketType> newTickets = new ArrayList<>();
                                if (occ.getTicketTypes() != null) {
                                        for (TicketType tt : occ.getTicketTypes()) {
                                                TicketType ntt = new TicketType();
                                                ntt.setEventOccurrence(nOcc);
                                                ntt.setName(tt.getName());
                                                ntt.setPrice(tt.getPrice());
                                                ntt.setQuantity(tt.getQuantity());
                                                newTickets.add(ntt);
                                        }
                                }
                                nOcc.setTicketTypes(newTickets);
                                newEvent.getEventOccurrences().add(nOcc);
                        }
                }

                Event saved = eventRepository.save(newEvent);
                return convertToDTO(saved);
        }

        @Override
        @Transactional
        public void bulkDelete(List<Long> ids) {
                List<Event> events = eventRepository.findAllById(ids);
                int deletedCount = 0;
                for (Event e : events) {
                        // Chỉ cho phép xóa sự kiện đã bị hủy hoặc từ chối
                        if (e.getStatus() == EventStatus.CANCELLED || e.getStatus() == EventStatus.REJECTED) {
                                e.setStatus(EventStatus.DELETED);
                                deletedCount++;
                        }
                }
                eventRepository.saveAll(events);

                if (deletedCount == 0) {
                        throw new IllegalStateException(
                                        "Không có sự kiện nào đủ điều kiện để xóa. Chỉ có thể xóa sự kiện đã bị hủy hoặc từ chối.");
                } else if (deletedCount < ids.size()) {
                        throw new IllegalStateException("Đã xóa " + deletedCount + "/" + ids.size()
                                        + " sự kiện. Chỉ có thể xóa sự kiện đã bị hủy hoặc từ chối.");
                }
        }

        @Override
        @Transactional
        public void bulkApprove(List<Long> ids) {
                List<Event> events = eventRepository.findAllById(ids);
                int approvedCount = 0;
                for (Event e : events) {
                        // Chỉ cho phép duyệt sự kiện đang chờ duyệt
                        if (e.getStatus() == EventStatus.PENDING) {
                                e.setStatus(EventStatus.APPROVED);
                                approvedCount++;
                                try {
                                        emailService.sendEventApprovalNotification(e);
                                } catch (Exception ex) {
                                        // ignore
                                }
                        }
                }
                eventRepository.saveAll(events);

                if (approvedCount == 0) {
                        throw new IllegalStateException(
                                        "Không có sự kiện nào đủ điều kiện để duyệt. Chỉ có thể duyệt sự kiện đang chờ duyệt.");
                } else if (approvedCount < ids.size()) {
                        throw new IllegalStateException("Đã duyệt " + approvedCount + "/" + ids.size()
                                        + " sự kiện. Chỉ có thể duyệt sự kiện đang chờ duyệt.");
                }
        }

        @Override
        @Transactional
        public void restore(Long eventId) {
                Event event = eventRepository.findById(eventId)
                                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại!"));

                // Kiểm tra điều kiện: chỉ khôi phục sự kiện đã bị xóa, hủy hoặc từ chối
                if (event.getStatus() != EventStatus.DELETED
                                && event.getStatus() != EventStatus.CANCELLED
                                && event.getStatus() != EventStatus.REJECTED) {
                        throw new IllegalStateException(
                                        "Chỉ có thể khôi phục sự kiện đã bị xóa, hủy hoặc từ chối. Trạng thái hiện tại: "
                                                        + event.getStatus());
                }

                // Lưu trạng thái cũ để ghi log
                EventStatus oldStatus = event.getStatus();

                // Khôi phục về trạng thái PENDING để admin xem xét lại
                event.setStatus(EventStatus.PENDING);
                eventRepository.save(event);

                // Ghi log lịch sử khôi phục
                log.info("Event #{} restored from {} to PENDING", eventId, oldStatus);

                // Gửi email thông báo cho nhà tổ chức
                if (emailService != null) {
                        try {
                                emailService.sendEventRestorationNotification(event);
                        } catch (Exception e) {
                                // Log but don't fail the transaction
                                log.error("Failed to send restoration email for event #{}: {}", eventId,
                                                e.getMessage());
                        }
                }
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
    @Transactional
    public void incrementViewCount(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null) {
            event.setViewCount(event.getViewCount() == null ? 1 : event.getViewCount() + 1);
            eventRepository.save(event);
        }
    }

    public com.codegym.appticket.dto.event.EventStatsDTO getEventStats(Long eventId, Long occurrenceId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // 1. Fetch Aggregated Stats
        java.util.List<Object[]> stats = eventRepository.sumRevenueAndTickets(eventId, occurrenceId);
        Long totalTickets = 0L;
        java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;

        if (stats != null && !stats.isEmpty()) {
            Object[] row = stats.get(0);
            if (row[0] != null)
                totalTickets = ((Number) row[0]).longValue();
            if (row[1] != null)
                totalRevenue = (java.math.BigDecimal) row[1];
        }

        // 2. Fetch Booked Tickets List
        List<com.codegym.appticket.dto.event.BookedTicketDTO> bookedTickets = eventRepository
                .findBookedTicketsByEventAndOccurrence(eventId, occurrenceId);

        return com.codegym.appticket.dto.event.EventStatsDTO.builder()
                .totalTicketsSold(totalTickets)
                .totalRevenue(totalRevenue)
                .viewCount(event.getViewCount() == null ? 0 : event.getViewCount())
                .bookedTickets(bookedTickets)
                .build();
    }

    @Override
    public byte[] exportBookedTicketsToExcel(Long eventId, Long occurrenceId) throws java.io.IOException {
        // Fetch data
        List<com.codegym.appticket.dto.event.BookedTicketDTO> tickets = eventRepository
                .findBookedTicketsByEventAndOccurrence(eventId, occurrenceId);

        // Create Workbook
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Danh sách vé");

            // Styles
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            org.apache.poi.ss.usermodel.CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

            org.apache.poi.ss.usermodel.CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0 ₫"));

            // Header Row
            String[] headers = {"Mã vé", "Khách hàng", "Email", "Số điện thoại", "Loại vé", "Giá vé", "Ngày đặt", "Suất diễn"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowNum = 1;
            java.math.BigDecimal totalRev = java.math.BigDecimal.ZERO;
            for (com.codegym.appticket.dto.event.BookedTicketDTO ticket : tickets) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(ticket.getTicketCode());
                row.createCell(1).setCellValue(ticket.getCustomerName());
                row.createCell(2).setCellValue(ticket.getCustomerEmail());
                row.createCell(3).setCellValue(ticket.getCustomerPhone());
                row.createCell(4).setCellValue(ticket.getTicketTypeName());

                org.apache.poi.ss.usermodel.Cell priceCell = row.createCell(5);
                priceCell.setCellValue(ticket.getTotalPrice().doubleValue());
                priceCell.setCellStyle(currencyStyle);

                if (ticket.getTotalPrice() != null) {
                    totalRev = totalRev.add(ticket.getTotalPrice());
                }

                org.apache.poi.ss.usermodel.Cell dateCell = row.createCell(6);
                if (ticket.getBookingTime() != null) {
                    dateCell.setCellValue(ticket.getBookingTime());
                }
                dateCell.setCellStyle(dateStyle);

                row.createCell(7).setCellValue(ticket.getOccurrenceTime());
            }

            // Total Row
            org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(4).setCellValue("Tổng tiền:");
            org.apache.poi.ss.usermodel.Cell totalVal = totalRow.createCell(5);
            totalVal.setCellValue(totalRev.doubleValue());
            totalVal.setCellStyle(currencyStyle);
            totalRow.getCell(5).setCellStyle(currencyStyle); // Apply again to be safe

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
