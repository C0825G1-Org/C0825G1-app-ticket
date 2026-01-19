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
            "AND e.status <> 'DELETED'")
    org.springframework.data.domain.Page<Event> searchEvents(@Param("title") String title,
            @Param("categoryId") Long categoryId,
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
                    e.location AS location,
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
                LEFT JOIN event_occurrences eo ON eo.event_id = e.id
                JOIN ticket_types tt ON tt.event_id = e.id
                JOIN booking_details bd ON bd.ticket_type_id = tt.id
                JOIN bookings b ON b.id = bd.booking_id
                WHERE b.status = 'SUCCESS'
                  AND e.status = 'APPROVED'
                GROUP BY e.id, e.title, e.description, e.location, c.name
                ORDER BY totalTickets DESC
                LIMIT 3
            """, nativeQuery = true)
    List<TrendingEventDTO> findTopTrendingEvents();

    @Query(value = """
                SELECT
                    e.id AS id,
                    e.title AS title,
                    e.description AS description,
                    e.location AS location,
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
                LEFT JOIN ticket_types tt ON tt.event_id = e.id
                WHERE eo.start_time > NOW()
                  AND e.status = 'APPROVED'
                GROUP BY e.id, e.title, e.description, e.location, c.name
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
                    e.location AS location,
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
                LEFT JOIN ticket_types tt ON tt.event_id = e.id
                WHERE e.status = 'APPROVED'
                GROUP BY e.id, e.title, e.description, e.location, c.name
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
                    e.location AS location,
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
                LEFT JOIN ticket_types tt ON tt.event_id = e.id
                WHERE e.status = 'APPROVED'
                  AND (:searchText IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :searchText, '%')))
                  AND (:categoryId IS NULL OR e.category_id = :categoryId)
                  AND (:location IS NULL OR e.location LIKE CONCAT('%', :location, '%'))
                GROUP BY e.id, e.title, e.description, e.location, c.name
                ORDER BY MIN(eo.start_time) ASC
            """, countQuery = """
                SELECT COUNT(DISTINCT e.id)
                FROM events e
                WHERE e.status = 'APPROVED'
                  AND (:searchText IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :searchText, '%')))
                  AND (:categoryId IS NULL OR e.category_id = :categoryId)
                  AND (:location IS NULL OR e.location LIKE CONCAT('%', :location, '%'))
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
                    e.location AS location,
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
                WHERE e.id = :eventId AND e.status = 'APPROVED'
                GROUP BY e.id, e.title, e.description, e.location, c.name
            """, nativeQuery = true)
    EventDetailDTO findEventDetailById(@Param("eventId") Long eventId);

    // Get ticket types for an event
    @Query(value = """
                SELECT
                    tt.id AS id,
                    tt.name AS name,
                    tt.price AS price,
                    (tt.quantity - COALESCE(SUM(bd.quantity), 0)) AS availableQuantity
                FROM ticket_types tt
                LEFT JOIN booking_details bd ON bd.ticket_type_id = tt.id
                LEFT JOIN bookings b ON b.id = bd.booking_id AND b.status = 'SUCCESS'
                WHERE tt.event_id = :eventId
                GROUP BY tt.id, tt.name, tt.price, tt.quantity
                HAVING availableQuantity > 0
                ORDER BY tt.price ASC
            """, nativeQuery = true)
    List<TicketTypeDTO> findTicketTypesByEventId(@Param("eventId") Long eventId);

    // Find events by Organizer (for User dashboard)
    org.springframework.data.domain.Page<Event> findByOrganizer(User organizer, Pageable pageable);

    // Find events by Organizer and Status (Optional, maybe useful later)
    org.springframework.data.domain.Page<Event> findByOrganizerAndStatusNot(User organizer,
            com.codegym.appticket.entity.EventStatus status, Pageable pageable);

    @Query(value = """
    SELECT 
        e.id AS id,
        e.title AS title,
        e.location AS location,
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
        MIN(et.start_time) AS eventDate
    FROM events e
    LEFT JOIN event_categories c ON c.id = e.category_id
    LEFT JOIN event_times et ON et.event_id = e.id
    WHERE e.status = 'APPROVED'
      AND e.latitude IS NOT NULL
      AND e.longitude IS NOT NULL
    GROUP BY e.id, e.title, e.location, e.latitude, e.longitude, c.name
    ORDER BY distance ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<NearByEventDTO> findNearbyEvents(
        @Param("userLat") Double userLatitude,
        @Param("userLon") Double userLongitude,
        @Param("limit") int limit
    );
}

