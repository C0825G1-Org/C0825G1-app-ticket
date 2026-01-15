package com.codegym.appticket.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho thống kê users trên dashboard
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {

    private long totalUsers;        // Tổng số user
    private long activeUsers;       // Đang hoạt động
    private long lockedUsers;       // Bị khóa
    private long newUsersThisMonth; // Mới tháng này
}
