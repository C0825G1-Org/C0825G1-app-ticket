package com.codegym.appticket.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO hiển thị thông tin user trong danh sách
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Boolean enabled;
    private Boolean isBlocked;
    private Boolean isDeleted;
    private Set<String> roleNames;
    private LocalDateTime createdDate;
}
