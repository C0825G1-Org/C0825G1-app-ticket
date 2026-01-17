package com.codegym.appticket.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLockHistoryDTO {
    private String actionType;
    private String reason;
    private LocalDateTime timestamp;
    private String createdBy;
}
