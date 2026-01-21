package com.codegym.appticket.dto.home;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NearByEventDTO {
    private Long id;
    private String title;
    private String location;
    private String image;
    private Double latitude;
    private Double longitude;
    private Double distance; // Khoảng cách tính bằng km
    private String categoryName;
    private LocalDateTime eventDate;
    private String addressDetail; // Địa chỉ chi tiết của location
    private Long occurrenceId; // ID của occurrence để link đến chi tiết
}