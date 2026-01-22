package com.codegym.appticket.repository;

import com.codegym.appticket.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Tìm kiếm user với keyword (tên, email, phone) + filter theo role và status
     * Chỉ lấy user chưa bị xóa (isDeleted = false hoặc null)
     * Status: ACTIVE = không bị khóa, BLOCKED = bị khóa
     */
    @Query("SELECT u FROM User u LEFT JOIN u.roles r WHERE " +
            "(u.isDeleted = false OR u.isDeleted IS NULL) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "   LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   u.phoneNumber LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:roleId IS NULL OR r.id = :roleId) AND " +
            "(NOT EXISTS (SELECT 1 FROM u.roles r2 WHERE r2.name = 'ADMIN')) AND " +
            "(:status IS NULL OR :status = '' OR " +
            "   (:status = 'ACTIVE' AND (u.isBlocked = false OR u.isBlocked IS NULL)) OR " +
            "   (:status = 'LOCKED' AND u.isBlocked = true))")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("roleId") Long roleId,
            @Param("status") String status,
            Pageable pageable);

    /**
     * Đếm tổng số user chưa bị xóa
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false OR u.isDeleted IS NULL")
    long countTotalUsers();

    /**
     * Đếm số user đang hoạt động (không bị khóa, chưa bị xóa)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isBlocked = false OR u.isBlocked IS NULL)")
    long countActiveUsers();

    /**
     * Đếm số user bị khóa (isBlocked = 1, chưa bị xóa)
     */
    @Query(value = "SELECT COUNT(*) FROM users WHERE (is_deleted = 0 OR is_deleted IS NULL) AND is_blocked = 1", nativeQuery = true)
    long countBlockedUsers();

    /**
     * Đếm số user mới trong tháng này
     */
    @Query("SELECT COUNT(u) FROM User u WHERE (u.isDeleted = false OR u.isDeleted IS NULL) AND u.createdDate >= :startOfMonth")
    long countNewUsersThisMonth(@Param("startOfMonth") LocalDateTime startOfMonth);

    /**
     * Tìm User chưa bị xóa
     */
    @Query(value = "select u from User u where u.email = :email and u.isDeleted is false")
    User findByEmailAndNotDeleted(@Param("email") String email);

    void deleteByEnabledFalseAndOtpExpiryBefore(LocalDateTime dateTime);

    java.util.List<User> findAllByIsBlockedTrueAndLockedAtBefore(LocalDateTime dateTime);

    /**
     * Tìm danh sách User có khả năng làm Organizer (ROLE_USER, không phải ADMIN)
     * Để hiển thị trong dropdown tạo sự kiện của Admin
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r " +
            "WHERE r.name = 'USER' " +
            "AND (u.isDeleted = false OR u.isDeleted IS NULL) " +
            "AND (u.isBlocked = false OR u.isBlocked IS NULL) " +
            "ORDER BY u.fullName ASC")
    java.util.List<User> findOrganizers();

    // --- Report Queries ---

    // 1. User Growth Chart (Group by Day)
    @Query(value = """
            SELECT DATE(created_at) as date, COUNT(*) as count
            FROM users
            WHERE created_at BETWEEN :start AND :end
              AND (is_deleted IS FALSE OR is_deleted IS NULL)
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at) ASC
            """, nativeQuery = true)
    java.util.List<Object[]> getUserGrowthStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 2. User Growth Chart (Group by Month)
    @Query(value = """
            SELECT DATE_FORMAT(created_at, '%Y-%m') as date, COUNT(*) as count
            FROM users
            WHERE created_at BETWEEN :start AND :end
              AND (is_deleted IS FALSE OR is_deleted IS NULL)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m')
            ORDER BY DATE_FORMAT(created_at, '%Y-%m') ASC
            """, nativeQuery = true)
    java.util.List<Object[]> getUserGrowthStatsByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 3. Top Organizers by Revenue (5% commission)
    @Query(value = """
            SELECT
                u.id AS id,
                u.full_name AS fullName,
                u.email AS email,
                COUNT(DISTINCT e.id) AS eventCount,
                COALESCE(SUM(CASE WHEN b.status = 'SUCCESS' AND b.booking_time BETWEEN :start AND :end THEN bd.quantity * tt.price ELSE 0 END) * 0.05, 0) AS totalRevenue
            FROM users u
            JOIN events e ON e.organizer_id = u.id
            LEFT JOIN event_occurrences eo ON eo.event_id = e.id
            LEFT JOIN ticket_types tt ON tt.event_occurrence_id = eo.id
            LEFT JOIN booking_details bd ON bd.ticket_type_id = tt.id
            LEFT JOIN bookings b ON b.id = bd.booking_id
            WHERE e.status = 'APPROVED'
            GROUP BY u.id, u.full_name, u.email
            ORDER BY totalRevenue DESC
            LIMIT :limit
            """, nativeQuery = true)
    java.util.List<com.codegym.appticket.dto.report.TopOrganizerDTO> findTopOrganizers(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit);

    // Count Users in period
    @Query("SELECT COUNT(u) FROM User u WHERE (u.isDeleted = false OR u.isDeleted IS NULL) AND u.createdDate BETWEEN :start AND :end")
    long countNewUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
