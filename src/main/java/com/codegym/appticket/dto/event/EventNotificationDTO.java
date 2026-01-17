package com.codegym.appticket.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventNotificationDTO {
    private Long id;
    private String title;
    private String createdBy;
    private LocalDateTime createdDate;
    private String timeAgo; // Calculated string like "5 phút trước"
}
