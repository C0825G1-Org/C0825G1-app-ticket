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
           "(:status IS NULL OR :status = '' OR " +
           "   (:status = 'ACTIVE' AND (u.isBlocked = false OR u.isBlocked IS NULL)) OR " +
           "   (:status = 'LOCKED' AND u.isBlocked = true))")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("roleId") Long roleId,
            @Param("status") String status,
            Pageable pageable
    );

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
}
