package com.codegym.appticket.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchDTO {
    private String title;
    private Long categoryId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate endDate;

    private com.codegym.appticket.entity.EventStatus status;
}
