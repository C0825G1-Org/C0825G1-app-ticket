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
                    MIN(p.name) AS location,
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
                  AND (e.status = 'APPROVED' OR e.status = 'HAPPENING')
                GROUP BY e.id, e.title, e.description, c.name
                ORDER BY totalTickets DESC
                LIMIT 3
            """, nativeQuery = true)
    List<TrendingEventDTO> findTopTrendingEvents();

    @Query(value = """
                SELECT
                    e.id AS id,
                    e.title AS title,
                    e.description AS description,
                    MIN(p.name) AS location,
                    COUNT(DISTINCT p.code) AS locationCount,
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
                  AND (e.status = 'APPROVED' OR e.status = 'HAPPENING')
                GROUP BY e.id, e.title, e.description, c.name
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
                    MIN(p.name) AS location,
                    COUNT(DISTINCT p.code) AS locationCount,
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
                WHERE (e.status = 'APPROVED' OR e.status = 'HAPPENING')
                GROUP BY e.id, e.title, e.description, c.name
                ORDER BY MIN(eo.start_time) ASC
            """, countQuery = """
                SELECT COUNT(DISTINCT e.id)
                FROM events e
                WHERE (e.status = 'APPROVED' OR e.status = 'HAPPENING')
            """, nativeQuery = true)
    Page<HomeEventDTO> findAllEvent(Pageable pageable);

    // Search events with filters, returns HomeEventDTO for display
    // Note: locationVariants should never be null - pass empty list if no location
    // filter
    @Query(value = """
                SELECT
                    e.id AS id,
                    e.title AS title,
                    e.description AS description,
                    MIN(p.name) AS location,
                    COUNT(DISTINCT p.code) AS locationCount,
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
                WHERE (e.status = 'APPROVED' OR e.status = 'HAPPENING')
                  AND (:searchText IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :searchText, '%')))
                  AND (:categoryId IS NULL OR e.category_id = :categoryId)
                  AND (:hasLocationFilter = 0 OR p.name IN :locationVariants)
                GROUP BY e.id, e.title, e.description, c.name
                ORDER BY MIN(eo.start_time) ASC
            """, countQuery = """
                SELECT COUNT(DISTINCT e.id)
                FROM events e
                LEFT JOIN event_occurrences eo ON eo.event_id = e.id
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                WHERE (e.status = 'APPROVED' OR e.status = 'HAPPENING')
                  AND (:searchText IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :searchText, '%')))
                  AND (:categoryId IS NULL OR e.category_id = :categoryId)
                  AND (:hasLocationFilter = 0 OR p.name IN :locationVariants)
            """, nativeQuery = true)
    Page<HomeEventDTO> searchHomeEvents(
            @Param("searchText") String searchText,
            @Param("categoryId") Long categoryId,
            @Param("locationVariants") List<String> locationVariants,
            @Param("hasLocationFilter") int hasLocationFilter,
            Pageable pageable);

    // Get event detail by ID
    @Query(value = """
                SELECT
                    e.id AS id,
                    e.title AS title,
                    e.description AS description,
                    MIN(p.name) AS location,
                    c.name AS categoryName,
                    (SELECT em.media_url
                     FROM event_media em
                     WHERE em.event_id = e.id
                     ORDER BY em.created_at ASC
                     LIMIT 1) AS mediaUrl,
                    e.status AS status,
                    MIN(eo.start_time) AS startTime,
                    MAX(eo.end_time) AS endTime
                FROM events e
                LEFT JOIN event_categories c ON c.id = e.category_id
                LEFT JOIN event_occurrences eo ON eo.event_id = e.id
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                WHERE e.id = :eventId AND (e.status = 'APPROVED' OR e.status = 'HAPPENING')
                GROUP BY e.id, e.title, e.description, c.name, e.status
            """, nativeQuery = true)
    EventDetailDTO findEventDetailById(@Param("eventId") Long eventId);

    // Get ticket types for an event (including sold-out tickets)
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
                LEFT JOIN bookings b ON b.id = bd.booking_id
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                WHERE eo.event_id = :eventId
                  AND (b.id IS NULL OR b.status = 'SUCCESS')
                GROUP BY tt.id, tt.name, tt.price, tt.quantity, eo.id, eo.start_time, p.name
                ORDER BY eo.start_time ASC, tt.price ASC
            """, nativeQuery = true)
    List<TicketTypeDTO> findTicketTypesByEventId(@Param("eventId") Long eventId);

    // Get all occurrences for an event
    @Query(value = """
                SELECT
                    eo.id AS id,
                    CONCAT(p.name, ', ', w.name) AS location,
                    l.address_detail AS addressDetail,
                    eo.start_time AS startTime,
                    eo.end_time AS endTime
                FROM event_occurrences eo
                LEFT JOIN locations l ON l.id = eo.location_id
                LEFT JOIN wards w ON w.code = l.ward_code
                LEFT JOIN provinces p ON p.code = w.province_code
                WHERE eo.event_id = :eventId
                ORDER BY eo.start_time ASC
            """, nativeQuery = true)
    List<com.codegym.appticket.dto.home.EventOccurrenceDisplayDTO> findOccurrencesByEventId(
            @Param("eventId") Long eventId);

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

    // 2. Events Count by Category (Pie Chart) - In Period
    @Query("SELECT e.category.name, COUNT(e) FROM Event e WHERE e.status = 'APPROVED' AND e.createdDate BETWEEN :start AND :end GROUP BY e.category.name")
    List<Object[]> countEventsByCategory(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdDate BETWEEN :start AND :end AND e.status = 'APPROVED'")
    long countNewEvents(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Find events that should transition to HAPPENING status (for scheduler)
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.eventOccurrences eo
            WHERE e.status = 'APPROVED'
            AND eo.startTime <= :now
            AND eo.endTime > :now
            """)
    List<Event> findStartedEvents(@Param("now") LocalDateTime now);

    // Find events that should transition to FINISHED status (for scheduler)
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.eventOccurrences eo
            WHERE e.status IN ('APPROVED', 'HAPPENING')
            AND eo.endTime <= :now
            GROUP BY e
            HAVING MAX(eo.endTime) <= :now
            """)
    List<Event> findFinishedEvents(@Param("now") LocalDateTime now);

    @Query(value = """
            SELECT
                e.id AS id,
                e.title AS title,
                CONCAT(p.name, ', ', w.name) AS location,
                (SELECT em.media_url
                 FROM event_media em
                 WHERE em.event_id = e.id
                 ORDER BY em.created_at ASC
                 LIMIT 1) AS image,
                l.latitude AS latitude,
                l.longitude AS longitude,
                (6371 * acos(
                    cos(radians(:userLat)) * cos(radians(l.latitude)) *
                    cos(radians(l.longitude) - radians(:userLon)) +
                    sin(radians(:userLat)) * sin(radians(l.latitude))
                )) AS distance,
                c.name AS categoryName,
                eo.start_time AS eventDate,
                l.address_detail AS addressDetail,
                eo.id AS occurrenceId
            FROM events e
            LEFT JOIN event_categories c ON c.id = e.category_id
            JOIN event_occurrences eo ON eo.event_id = e.id
            JOIN locations l ON l.id = eo.location_id
            LEFT JOIN wards w ON w.code = l.ward_code
            LEFT JOIN provinces p ON p.code = w.province_code
            WHERE e.status = 'APPROVED' OR e.status = 'HAPPENING'
              AND l.latitude IS NOT NULL
              AND l.longitude IS NOT NULL
              AND eo.start_time > NOW()
              AND (:hasExcludeFilter = 0 OR p.name NOT IN :excludeLocationVariants)
            HAVING distance > 0 AND distance < 160
            ORDER BY distance ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<NearByEventDTO> findNearbyEvents(
            @Param("userLat") Double userLatitude,
            @Param("userLon") Double userLongitude,
            @Param("excludeLocationVariants") List<String> excludeLocationVariants,
            @Param("hasExcludeFilter") int hasExcludeFilter,
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

    @Query(value = """
            SELECT
                e.id AS id,
                e.title AS title,
                MIN(eo.start_time) AS startTime,
                COALESCE(SUM(CASE WHEN b.status = 'SUCCESS' THEN bd.quantity ELSE 0 END), 0) AS soldTickets,
                (COALESCE(SUM(CASE WHEN b.status = 'SUCCESS' THEN bd.quantity ELSE 0 END), 0) + SUM(tt.quantity)) AS totalTickets
            FROM events e
            JOIN event_occurrences eo ON eo.event_id = e.id
            JOIN ticket_types tt ON tt.event_occurrence_id = eo.id
            LEFT JOIN booking_details bd ON bd.ticket_type_id = tt.id
            LEFT JOIN bookings b ON b.id = bd.booking_id
            WHERE e.status = 'APPROVED'
            GROUP BY e.id, e.title
            HAVING MIN(eo.start_time) > NOW()
            ORDER BY MIN(eo.start_time) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<com.codegym.appticket.dto.admin.EventSalesDTO> findTopUpcomingEventsWithSales(@Param("limit") int limit);
    // --- Organizer Stats Queries ---

    @Query(value = """
            SELECT
                SUM(bd.quantity) AS totalTicketsSold,
                SUM(bd.quantity * tt.price) AS totalRevenue
            FROM bookings b
            JOIN booking_details bd ON bd.booking_id = b.id
            JOIN ticket_types tt ON bd.ticket_type_id = tt.id
            JOIN event_occurrences eo ON tt.event_occurrence_id = eo.id
            WHERE b.status = 'SUCCESS'
              AND eo.event_id = :eventId
              AND (:occurrenceId IS NULL OR eo.id = :occurrenceId)
            """, nativeQuery = true)
    java.util.List<Object[]> sumRevenueAndTickets(@Param("eventId") Long eventId, @Param("occurrenceId") Long occurrenceId);


    @Query("""
            SELECT new com.codegym.appticket.dto.event.BookedTicketDTO(
                t.ticketCode,
                u.fullName,
                u.email,
                u.phoneNumber,
                tt.name,
                bd.quantity,
                CAST(bd.quantity * tt.price AS bigdecimal),
                b.bookingTime,
                CAST(b.status AS string),
                CONCAT(eo.startTime, ' - ', l.addressDetail)
            )
            FROM Booking b
            JOIN b.bookingDetails bd
            JOIN bd.tickets t
            JOIN bd.ticketType tt
            JOIN tt.eventOccurrence eo
            JOIN eo.location l
            JOIN b.user u
            WHERE b.status = 'SUCCESS'
              AND eo.event.id = :eventId
              AND (:occurrenceId IS NULL OR eo.id = :occurrenceId)
            ORDER BY b.bookingTime DESC
            """)
    List<com.codegym.appticket.dto.event.BookedTicketDTO> findBookedTicketsByEventAndOccurrence(@Param("eventId") Long eventId, @Param("occurrenceId") Long occurrenceId);

    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.bookingDetail.ticketType.eventOccurrence eo WHERE eo.event.id = :eventId AND t.used = true")
    Long countCheckedInTickets(@Param("eventId") Long eventId);
}
