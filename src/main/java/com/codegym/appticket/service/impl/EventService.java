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
import com.codegym.appticket.repository.IEventCategoryRepository;
import com.codegym.appticket.repository.IEventMediaRepository;
import com.codegym.appticket.repository.IEventRepository;
import com.codegym.appticket.repository.ILocationRepository;
import com.codegym.appticket.repository.IProvinceRepository;
import com.codegym.appticket.repository.ITicketTypeRepository;

import com.codegym.appticket.repository.IWardRepository;
import com.codegym.appticket.repository.IEventOccurrenceRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.codegym.appticket.service.IEventService;
import com.codegym.appticket.service.IGeoLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                            start,
                            end,
                            pageable).map(this::convertToDTO);
    }

    private void validateBusinessRules(List<EventOccurrenceDTO> occurrences, List<TicketTypeDTO> tickets,
                    Long currentEventId) {
            // 1. Validate Location & Time
            for (EventOccurrenceDTO occ : occurrences) {
                    Ward ward = wardRepository.findById(occ.getWardCode()).orElse(null);

                    if (ward != null) {
                            // Existing Ward: Validate Hierarchy
                            if (!ward.getProvince().getCode().equals(occ.getProvinceCode())) {
                                    throw new RuntimeException("Phường/Xã không thuộc Tỉnh/Thành phố đã chọn");
                            }
                    } else {
                            // New Ward: Must have name to create later
                            if (occ.getWardName() == null || occ.getWardName().trim().isEmpty()) {
                                    throw new RuntimeException("Không tìm thấy dữ liệu Phường/Xã");
                            }
                            // Cannot validate hierarchy against DB yet, trusting Frontend API
                    }

                    if (occ.getStartTime().isBefore(LocalDateTime.now().plusDays(3))) {
                            throw new RuntimeException("Thời gian bắt đầu phải sau ít nhất 3 ngày từ hiện tại");
                    }
                    if (occ.getEndTime().isBefore(occ.getStartTime().plusMinutes(30))) {
                            throw new RuntimeException(
                                            "Thời gian kết thúc phải sau thời gian bắt đầu ít nhất 30 phút");
                    }

                    // Conflict Check
                    locationRepository.findByWardCodeAndAddressDetail(occ.getWardCode(), occ.getAddressDetail())
                                    .ifPresent(loc -> {
                                            List<EventOccurrence> conflicts = eventOccurrenceRepository
                                                            .findConflicts(loc.getId(), occ.getStartTime(),
                                                                            occ.getEndTime());
                                            if (conflicts.stream().anyMatch(c -> currentEventId == null
                                                            || !c.getEvent().getId().equals(currentEventId))) {
                                                    throw new RuntimeException(
                                                                    "Xung đột lịch trình: Đã có sự kiện diễn ra tại địa điểm này trong khoảng thời gian đã chọn");
                                            }
                                    });
            }

            // 2. Validate Tickets
            Set<String> ticketNames = new HashSet<>();
            for (TicketTypeDTO t : tickets) {
                    if (!ticketNames.add(t.getName().toLowerCase().trim())) {
                            throw new RuntimeException("Tên loại vé '" + t.getName() + "' bị trùng lặp");
                    }
                    if (t.getPrice().compareTo(BigDecimal.ZERO) > 0
                                    && t.getPrice().compareTo(new BigDecimal("10000")) < 0) {
                            throw new RuntimeException("Giá vé phải bằng 0 hoặc tối thiểu 10.000 VNĐ");
                    }
                    // Manual 'Not Blank' check if needed, but DTO @NotBlank handles it.
            }
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
                 currentEmail = ((com.codegym.appticket.dto.user.UserInfoUserDetails) auth.getPrincipal()).getUsername();
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
            validateBusinessRules(dto.getEventOccurrences(), dto.getTicketTypes(), null);

            Event savedEvent = eventRepository.save(event);

            // Tạo các lần diễn ra sự kiện
            if (dto.getEventOccurrences() != null && !dto.getEventOccurrences().isEmpty()) {
                    List<EventOccurrence> occurrences = dto.getEventOccurrences()
                                    .stream()
                                    .map(occDTO -> {
                                            EventOccurrence occurrence = new EventOccurrence();
                                            occurrence.setEvent(savedEvent);
                                            occurrence.setStartTime(occDTO.getStartTime());
                                            occurrence.setEndTime(occDTO.getEndTime());
                                            occurrence.setLocation(getOrCreateLocation(occDTO));
                                            return occurrence;
                                    })
                                    .collect(Collectors.toList());
                    savedEvent.getEventOccurrences().addAll(occurrences); // Lưu cascade
            }

            // Tạo TicketTypes
            if (dto.getTicketTypes() != null && !dto.getTicketTypes().isEmpty()) {
                    List<TicketType> ticketTypes = dto.getTicketTypes().stream()
                                    .map(ticketTypeDTO -> {
                                            TicketType ticketType = new TicketType();
                                            ticketType.setEvent(savedEvent);
                                            ticketType.setName(ticketTypeDTO.getName());
                                            ticketType.setPrice(ticketTypeDTO.getPrice());
                                            ticketType.setQuantity(ticketTypeDTO.getQuantity());
                                            return ticketType;
                                    })
                                    .collect(Collectors.toList());
                    ticketTypeRepository.saveAll(ticketTypes);
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
            validateBusinessRules(dto.getEventOccurrences(), dto.getTicketTypes(), id);

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
                    // Find earliest start time
                    LocalDateTime earliestStart = event.getEventOccurrences().stream()
                                    .map(EventOccurrence::getStartTime)
                                    .min(LocalDateTime::compareTo)
                                    .orElse(null);

                    if (earliestStart != null) {
                            // Check if now is within 48 hours of start (or past it)
                            // Rule: "trước 2 ngày tổ chức thì mới được sửa" => Cannot edit if time < 48h
                            long hoursUntilStart = java.time.Duration.between(LocalDateTime.now(), earliestStart)
                                            .toHours();
                            if (hoursUntilStart < 48) {
                                    throw new RuntimeException(
                                                    "Không thể chỉnh sửa sự kiện đã duyệt trong vòng 48 giờ trước khi bắt đầu.");
                            }
                    }
            }
            // ---------------------------

            // Cập nhật các lần diễn ra sự kiện: Merge logic (Tránh lỗi Duplicate Entry)
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

                        if (isNew) {
                                currentOccurrences.add(target);
                        }
                }

            // Cập nhật các loại vé: Cập nhật thông minh (Cập nhật hiện có, Tạo mới, Xóa đã
            // bỏ)
            List<TicketType> currentTicketTypes = ticketTypeRepository
                            .findByEventId(id);
            List<TicketTypeDTO> incomingTicketTypes = dto.getTicketTypes() != null
                            ? dto.getTicketTypes()
                            : new ArrayList<>();

            // 1. Xác định các loại vé cần xóa (có trong DB nhưng không có trong DTO)
            // Lưu ý: Chỉ xóa nếu chúng thực sự biến mất khỏi giao diện.
            // Tuy nhiên, việc xóa có thể thất bại nếu đã có vé đặt. Lý tưởng nhất là bắt
            // ngoại lệ hoặc cứ để lại?
            // Để hệ thống chặt chẽ, thử xóa. Nếu lỗi khóa ngoại, ném ngoại lệ hoặc bỏ qua.
            // Thử xóa chúng.
            List<Long> incomingIds = incomingTicketTypes.stream()
                            .map(TicketTypeDTO::getId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            List<TicketType> toDelete = currentTicketTypes.stream()
                            .filter(tt -> !incomingIds.contains(tt.getId()))
                            .collect(Collectors.toList());

            if (!toDelete.isEmpty()) {
                    ticketTypeRepository.deleteAll(toDelete);
            }

            // 2. Cập nhật hiện có & Tạo mới
            List<TicketType> toSave = new ArrayList<>();
            for (TicketTypeDTO ttDto : incomingTicketTypes) {
                    TicketType ticketType;

                    if (ttDto.getId() != null) {
                            // Cập nhật hiện có
                            ticketType = currentTicketTypes.stream()
                                            .filter(tt -> tt.getId().equals(ttDto.getId()))
                                            .findFirst()
                                            .orElseThrow(() -> new RuntimeException(
                                                            "Không tìm thấy loại vé với ID: " + ttDto.getId()));
                    } else {
                            // Tạo mới
                            ticketType = new TicketType();
                            ticketType.setEvent(event);
                    }

                    ticketType.setName(ttDto.getName());
                    ticketType.setPrice(ttDto.getPrice());
                    ticketType.setQuantity(ttDto.getQuantity());
                    toSave.add(ticketType);
            }
            ticketTypeRepository.saveAll(toSave);

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
                                    // Quận/Huyện đã bị xóa theo đặc tả OpenApi v2 (hệ thống phân cấp 2 cấp dơn
                                    // giản)

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

                            .eventOccurrences(occurrenceDTOs)
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

            return eventRepository.searchHomeEvents(searchText, categoryId, locationVariants, hasLocationFilter, pageable);
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
    }

    @Override
    @Transactional
    public void reject(Long id, String reason) {
            Event event = eventRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Event not found with id " + id));
            event.setStatus(EventStatus.REJECTED);
            // We can add logic to save reason later or send notification
            eventRepository.save(event);
    }

    @Override
    public List<NearByEventDTO> findNearbyEvents(Double userLatitude, Double userLongitude, String excludeLocation, int limit) {
        // Convert exclude location to variants list
        List<String> excludeLocationVariants = new ArrayList<>();
        int hasExcludeFilter = 0;

        if (excludeLocation != null && !excludeLocation.trim().isEmpty()) {
            excludeLocationVariants = provinceNameMapper.getProvinceVariants(excludeLocation);
            hasExcludeFilter = excludeLocationVariants.isEmpty() ? 0 : 1;
        }

        return eventRepository.findNearbyEvents(userLatitude, userLongitude, excludeLocationVariants, hasExcludeFilter, limit);
    }

    @Override
    public List<NearByEventWithOccurrencesDTO> findNearbyEventsGrouped(Double userLatitude, Double userLongitude, String excludeLocation, int limit) {
        // Convert exclude location to variants list
        List<String> excludeLocationVariants = new ArrayList<>();
        int hasExcludeFilter = 0;

        if (excludeLocation != null && !excludeLocation.trim().isEmpty()) {
            excludeLocationVariants = provinceNameMapper.getProvinceVariants(excludeLocation);
            hasExcludeFilter = excludeLocationVariants.isEmpty() ? 0 : 1;
        }

        // Get all nearby event occurrences from repository
        List<NearByEventDTO> allOccurrences = eventRepository.findNearbyEvents(userLatitude, userLongitude, excludeLocationVariants, hasExcludeFilter, limit * 3);

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
        public long countAll() {
                return eventRepository.count();
        }
}
