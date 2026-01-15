package com.codegym.appticket.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * DTO cho trang xem chi tiết user
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDTO {

    // Thông tin cơ bản
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Boolean enabled;
    private Boolean isBlocked;
    private Boolean isDeleted;
    private Set<String> roleNames;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;

    // Thống kê
    private Long ticketCount;       // Số vé đã mua
    private Long eventCount;        // Số sự kiện đã tạo
    private String totalSpent;      // Tổng chi tiêu (formatted)

    // Lịch sử hoạt động
    private List<ActivityDTO> activities;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDTO {
        private String type;        // BOOKING, EVENT_CREATED
        private String description; // "Đặt vé EDM Symphony Night" hoặc "Tạo sự kiện Workshop AI"
        private LocalDateTime timestamp;
    }
}
