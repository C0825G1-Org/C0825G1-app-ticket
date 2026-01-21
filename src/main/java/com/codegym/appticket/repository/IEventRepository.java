package com.codegym.appticket.repository;

import com.codegym.appticket.dto.home.EventDetailDTO;
import com.codegym.appticket.dto.home.HomeEventDTO;
import com.codegym.appticket.dto.home.NearByEventDTO;
import com.codegym.appticket.dto.home.TicketTypeDTO;
import com.codegym.appticket.dto.home.TrendingEventDTO;
import com.codegym.appticket.dto.home.UpComingEventDTO;
import com.codegym.appticket.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import com.codegym.appticket.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface IEventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN e.eventOccurrences t " +
            "WHERE (:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:categoryId IS NULL OR e.category.id = :categoryId) " +
            "AND (:startDateTime IS NULL OR t.startTime >= :startDateTime) " +
            "AND (:endDateTime IS NULL OR t.endTime <= :endDateTime) " +
            "AND ((:status IS NOT NULL AND e.status = :status) OR (:status IS NULL AND e.status <> 'DELETED'))")
    org.springframework.data.domain.Page<Event> searchEvents(@Param("title") String title,
            @Param("categoryId") Long categoryId,
            @Param("status") com.codegym.appticket.entity.EventStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            org.springframework.data.domain.Pageable pageable);

    // Tìm tất cả ngoại trừ trạng thái đã xóa (cho admin list mặc định)
    org.springframework.data.domain.Page<Event> findByStatusNot(com.codegym.appticket.entity.EventStatus status,
            Pageable pageable);

    // Đếm số sự kiện user đã tạo
    long countByCreatedBy(User createdBy);

    // Đếm số sự kiện theo trạng thái (cho admin dashboard/notification)
    long countByStatus(com.codegym.appticket.entity.EventStatus status);

    long countByStatusIn(java.util.Collection<com.codegym.appticket.entity.EventStatus> statuses);

    // Lấy danh sách sự kiện user đã tạo, sắp xếp mới nhất
    List<Event> findByCreatedByOrderByCreatedDateDesc(User createdBy, Pageable pageable);

    // Lấy top 10 sự kiện mới nhất theo trạng thái (cho notification)
    List<Event> findTop10ByStatusOrderByCreatedDateDesc(com.codegym.appticket.entity.EventStatus status);

    // Tìm kiếm theo status (tách biệt để không sửa hàm search cũ)
    org.springframework.data.domain.Page<Event> findByStatus(com.codegym.appticket.entity.EventStatus status,
            Pageable pageable);

    @Query(value = """
                SELECT
                    e.id AS id,
                    e.title AS title,
                    e.description AS description,
                    p.name AS location,
                    (SELECT em.media_url
                     FROM event_media em
                     WHERE em.event_id = e.id
                     ORDER BY em.created_at ASC
                     LIMIT 1) AS image,
                    MIN(eo.start_time) AS eventDate,
                    c.name AS categoryName,
                    SUM(bd.quantity) AS totalTickets
                FROM events e
                LEFT JOIN event_categories c ON c.id = e.category_id
                JOIN event_occurrences eo ON eo.event_id = e.id
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                JOIN ticket_types tt ON tt.event_occurrence_id = eo.id
                JOIN booking_details bd ON bd.ticket_type_id = tt.id
                JOIN bookings b ON b.id = bd.booking_id
                WHERE b.status = 'SUCCESS'
                  AND e.status = 'APPROVED'
                GROUP BY e.id, e.title, e.description, p.name, c.name
                ORDER BY totalTickets DESC
                LIMIT 3
            """, nativeQuery = true)
    List<TrendingEventDTO> findTopTrendingEvents();

    @Query(value = """
                SELECT
                    e.id AS id,
                    e.title AS title,
                    e.description AS description,
                    p.name AS location,
                    (SELECT em.media_url
                     FROM event_media em
                     WHERE em.event_id = e.id
                     ORDER BY em.created_at ASC
                     LIMIT 1) AS image,
                    c.name AS categoryName,
                    MIN(eo.start_time) AS startTime,
                    MIN(tt.price) AS minPrice
                FROM events e
                LEFT JOIN event_categories c ON c.id = e.category_id
                JOIN event_occurrences eo ON eo.event_id = e.id
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                LEFT JOIN ticket_types tt ON tt.event_occurrence_id = eo.id
                WHERE eo.start_time > NOW()
                  AND e.status = 'APPROVED'
                GROUP BY e.id, e.title, e.description, p.name, c.name
                ORDER BY startTime ASC
                LIMIT 4
            """, nativeQuery = true)
    // @Query("""
    // SELECT e.id AS id,
    // e.title AS title,
    // e.location AS location,
    // MIN(et.startTime) AS startTime
    // FROM Event e
    // JOIN e.eventTimes et
    // WHERE et.startTime > CURRENT_TIMESTAMP
    // AND e.status = 'APPROVED'
    // GROUP BY e.id, e.title, e.location
    // ORDER BY startTime ASC
    // """)
    List<UpComingEventDTO> findUpComingEvents();

    @Query(value = """
                SELECT
                    e.id AS id,
                    e.title AS title,
                    e.description AS description,
                    p.name AS location,
                    (SELECT em.media_url
                     FROM event_media em
                     WHERE em.event_id = e.id
                     ORDER BY em.created_at ASC
                     LIMIT 1) AS mediaUrl,
                    c.name AS categoryName,
                    MIN(eo.start_time) AS startTime,
                    MIN(tt.price) AS price
                FROM events e
                LEFT JOIN event_categories c ON c.id = e.category_id
                LEFT JOIN event_occurrences eo ON eo.event_id = e.id
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                LEFT JOIN ticket_types tt ON tt.event_occurrence_id = eo.id
                WHERE e.status = 'APPROVED'
                GROUP BY e.id, e.title, e.description, p.name, c.name
                ORDER BY MIN(eo.start_time) ASC
            """, countQuery = """
                SELECT COUNT(DISTINCT e.id)
                FROM events e
                WHERE e.status = 'APPROVED'
            """, nativeQuery = true)
    Page<HomeEventDTO> findAllEvent(Pageable pageable);

    // Search events with filters, returns HomeEventDTO for display
    @Query(value = """
                SELECT
                    e.id AS id,
                    e.title AS title,
                    e.description AS description,
                    p.name AS location,
                    (SELECT em.media_url
                     FROM event_media em
                     WHERE em.event_id = e.id
                     ORDER BY em.created_at ASC
                     LIMIT 1) AS mediaUrl,
                    c.name AS categoryName,
                    MIN(eo.start_time) AS startTime,
                    MIN(tt.price) AS price
                FROM events e
                LEFT JOIN event_categories c ON c.id = e.category_id
                LEFT JOIN event_occurrences eo ON eo.event_id = e.id
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                LEFT JOIN ticket_types tt ON tt.event_occurrence_id = eo.id
                WHERE e.status = 'APPROVED'
                  AND (:searchText IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :searchText, '%')))
                  AND (:categoryId IS NULL OR e.category_id = :categoryId)
                  AND (:location IS NULL OR
                       p.name LIKE CONCAT('%', :location, '%') OR
                       (:location = 'Hồ Chí Minh' AND (p.name LIKE '%TP.HCM%' OR p.name LIKE '%HCM%' OR p.name LIKE '%Hồ Chí Minh%' OR p.name LIKE '%Thành phố Hồ Chí Minh%')) OR
                       (:location = 'Hà Nội' AND (p.name LIKE '%Hà Nội%' OR p.name LIKE '%Ha Noi%' OR p.name LIKE '%Hanoi%' OR p.name LIKE '%Thành phố Hà Nội%')) OR
                       (:location = 'Đà Nẵng' AND (p.name LIKE '%Đà Nẵng%' OR p.name LIKE '%Da Nang%' OR p.name LIKE '%Danang%' OR p.name LIKE '%Thành phố Đà Nẵng%'))
                      )
                GROUP BY e.id, e.title, e.description, p.name, c.name
                ORDER BY MIN(eo.start_time) ASC
            """, countQuery = """
                SELECT COUNT(DISTINCT e.id)
                FROM events e
                LEFT JOIN event_occurrences eo ON eo.event_id = e.id
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                WHERE e.status = 'APPROVED'
                  AND (:searchText IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :searchText, '%')))
                  AND (:categoryId IS NULL OR e.category_id = :categoryId)
                  AND (:location IS NULL OR
                       p.name LIKE CONCAT('%', :location, '%') OR
                       (:location = 'Hồ Chí Minh' AND (p.name LIKE '%TP.HCM%' OR p.name LIKE '%HCM%' OR p.name LIKE '%Hồ Chí Minh%' OR p.name LIKE '%Thành phố Hồ Chí Minh%')) OR
                       (:location = 'Hà Nội' AND (p.name LIKE '%Hà Nội%' OR p.name LIKE '%Ha Noi%' OR p.name LIKE '%Hanoi%' OR p.name LIKE '%Thành phố Hà Nội%')) OR
                       (:location = 'Đà Nẵng' AND (p.name LIKE '%Đà Nẵng%' OR p.name LIKE '%Da Nang%' OR p.name LIKE '%Danang%' OR p.name LIKE '%Thành phố Đà Nẵng%'))
                      )
            """, nativeQuery = true)
    Page<HomeEventDTO> searchHomeEvents(
            @Param("searchText") String searchText,
            @Param("categoryId") Long categoryId,
            @Param("location") String location,
            Pageable pageable);

    // Get event detail by ID
    @Query(value = """
                SELECT
                    e.id AS id,
                    e.title AS title,
                    e.description AS description,
                    p.name AS location,
                    c.name AS categoryName,
                    (SELECT em.media_url
                     FROM event_media em
                     WHERE em.event_id = e.id
                     ORDER BY em.created_at ASC
                     LIMIT 1) AS mediaUrl,
                    MIN(eo.start_time) AS startTime,
                    MAX(eo.end_time) AS endTime
                FROM events e
                LEFT JOIN event_categories c ON c.id = e.category_id
                LEFT JOIN event_occurrences eo ON eo.event_id = e.id
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                WHERE e.id = :eventId AND e.status = 'APPROVED'
                GROUP BY e.id, e.title, e.description, p.name, c.name
            """, nativeQuery = true)
    EventDetailDTO findEventDetailById(@Param("eventId") Long eventId);

    // Get ticket types for an event
    // Get ticket types for an event, detailed with occurrence info
    @Query(value = """
                SELECT
                    tt.id AS id,
                    tt.name AS name,
                    tt.price AS price,
                    (tt.quantity - COALESCE(SUM(bd.quantity), 0)) AS availableQuantity,
                    eo.id AS occurrenceId,
                    eo.start_time AS startTime,
                    p.name AS location
                FROM ticket_types tt
                JOIN event_occurrences eo ON tt.event_occurrence_id = eo.id
                LEFT JOIN booking_details bd ON bd.ticket_type_id = tt.id
                LEFT JOIN bookings b ON b.id = bd.booking_id AND b.status = 'SUCCESS'
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                WHERE eo.event_id = :eventId
                GROUP BY tt.id, tt.name, tt.price, tt.quantity, eo.id, eo.start_time, p.name
                HAVING availableQuantity > 0
                ORDER BY eo.start_time ASC, tt.price ASC
            """, nativeQuery = true)
    List<TicketTypeDTO> findTicketTypesByEventId(@Param("eventId") Long eventId);

    // Find events by Organizer (for User dashboard)
    org.springframework.data.domain.Page<Event> findByOrganizer(User organizer, Pageable pageable);

    // Find events by Organizer and Status (Optional, maybe useful later)
    org.springframework.data.domain.Page<Event> findByOrganizerAndStatusNot(User organizer,
            com.codegym.appticket.entity.EventStatus status, Pageable pageable);

    // --- Report Queries ---

    // 1. Top Selling Events (by Ticket Quantity or Revenue)
    @Query(value = """
            SELECT
                e.id AS id,
                e.title AS title,
                c.name AS categoryName,
                SUM(bd.quantity) AS ticketsSold,
                SUM(bd.quantity * tt.price) * 0.05 AS revenue
            FROM events e
            JOIN event_categories c ON c.id = e.category_id
            JOIN event_occurrences eo ON eo.event_id = e.id
            JOIN ticket_types tt ON tt.event_occurrence_id = eo.id
            JOIN booking_details bd ON bd.ticket_type_id = tt.id
            JOIN bookings b ON b.id = bd.booking_id
            WHERE b.status = 'SUCCESS'
              AND e.status = 'APPROVED'
              AND (b.booking_time BETWEEN :start AND :end)
            GROUP BY e.id, e.title, c.name
            ORDER BY revenue DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<com.codegym.appticket.dto.report.TopEventDTO> findTopSellingEvents(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit);

    // 2. Events Count by Category (Pie Chart)
    @Query("SELECT e.category.name, COUNT(e) FROM Event e WHERE e.status = 'APPROVED' GROUP BY e.category.name")
    List<Object[]> countEventsByCategory();

    // 3. Count Events Created in Period
    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdDate BETWEEN :start AND :end AND e.status = 'APPROVED'")
    long countNewEvents(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT
                e.id AS id,
                e.title AS title,
                p.name AS location,
                (SELECT em.media_url
                 FROM event_media em
                 WHERE em.event_id = e.id
                 ORDER BY em.created_at ASC
                 LIMIT 1) AS image,
                e.latitude AS latitude,
                e.longitude AS longitude,
                (6371 * acos(
                    cos(radians(:userLat)) * cos(radians(e.latitude)) *
                    cos(radians(e.longitude) - radians(:userLon)) +
                    sin(radians(:userLat)) * sin(radians(e.latitude))
                )) AS distance,
                c.name AS categoryName,
                MIN(eo.start_time) AS eventDate
            FROM events e
            LEFT JOIN event_categories c ON c.id = e.category_id
            LEFT JOIN event_occurrences eo ON eo.event_id = e.id
            LEFT JOIN locations l ON l.id = eo.location_id
            LEFT JOIN wards w ON w.code = l.ward_code
            LEFT JOIN provinces p ON p.code = w.province_code
            WHERE e.status = 'APPROVED'
              AND e.latitude IS NOT NULL
              AND e.longitude IS NOT NULL
              AND NOT (
                  (:excludeLocation = 'Hồ Chí Minh' AND (p.name LIKE '%TP.HCM%' OR p.name LIKE '%HCM%' OR p.name LIKE '%Hồ Chí Minh%' OR p.name LIKE '%Thành phố Hồ Chí Minh%')) OR
                  (:excludeLocation = 'Hà Nội' AND (p.name LIKE '%Hà Nội%' OR p.name LIKE '%Ha Noi%' OR p.name LIKE '%Hanoi%' OR p.name LIKE '%Thành phố Hà Nội%')) OR
                  (:excludeLocation = 'Đà Nẵng' AND (p.name LIKE '%Đà Nẵng%' OR p.name LIKE '%Da Nang%' OR p.name LIKE '%Danang%' OR p.name LIKE '%Thành phố Đà Nẵng%')) OR
                  (p.name LIKE CONCAT('%', :excludeLocation, '%'))
              )
            GROUP BY e.id, e.title, p.name, e.latitude, e.longitude, c.name
            HAVING distance < 160
            ORDER BY distance ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<NearByEventDTO> findNearbyEvents(
            @Param("userLat") Double userLatitude,
            @Param("userLon") Double userLongitude,
            @Param("excludeLocation") String excludeLocation,
            @Param("limit") int limit);

    @Query(value = """
            SELECT
                e.id AS id,
                e.title AS title,
                p.name AS location,
                (SELECT em.media_url FROM event_media em
                 WHERE em.event_id = e.id
                 ORDER BY em.created_at ASC LIMIT 1) AS image,
                c.name AS categoryName,
                MIN(eo.start_time) AS eventDate
            FROM events e
            LEFT JOIN event_categories c ON c.id = e.category_id
            LEFT JOIN event_occurrences eo ON eo.event_id = e.id
            LEFT JOIN locations l ON l.id = eo.location_id
            LEFT JOIN wards w ON w.code = l.ward_code
            LEFT JOIN provinces p ON p.code = w.province_code
            WHERE e.status = 'APPROVED'
              AND p.name IN :nearbyProvinces
            GROUP BY e.id, e.title, p.name, c.name
            ORDER BY MIN(eo.start_time) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<NearByEventDTO> findEventsByProvinces(
            @Param("nearbyProvinces") List<String> nearbyProvinces,
            @Param("limit") int limit);

    @Query("SELECT e FROM Event e " +
            "WHERE e.status IN ('APPROVED', 'HAPPENING') " +
            "AND NOT EXISTS (SELECT o FROM EventOccurrence o WHERE o.event = e AND o.endTime >= :now)")
    List<Event> findFinishedEvents(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e " +
            "WHERE e.status = 'APPROVED' " +
            "AND EXISTS (SELECT o FROM EventOccurrence o WHERE o.event = e AND o.startTime <= :now)")
    List<Event> findStartedEvents(@Param("now") LocalDateTime now);
}
