package com.codegym.appticket.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho search và filter users
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDTO {

    private String keyword;      // Tìm theo tên, email, phone
    private Long roleId;         // Filter theo role
    private String status;       // ACTIVE, LOCKED
    private Integer page = 0;
    private Integer size = 10;
}
