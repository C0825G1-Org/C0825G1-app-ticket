package com.codegym.appticket.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;
import java.util.ArrayList;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOccurrenceDTO {

    private Long id;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    // Flattened Location Info
    // Codes are required for mapping
    @NotNull(message = "Vui lòng chọn Tỉnh/Thành phố")
    private Integer provinceCode;
    private String provinceName; // To save if new

    @NotNull(message = "Vui lòng chọn Phường/Xã")
    private Integer wardCode;
    private String wardName; // To save if new

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Size(min = 5, message = "Địa chỉ chi tiết quá ngắn (tối thiểu 5 ký tự)")
    private String addressDetail;

    private String mapLink;

    @Builder.Default
    private List<TicketTypeDTO> ticketTypes = new ArrayList<>();
}
