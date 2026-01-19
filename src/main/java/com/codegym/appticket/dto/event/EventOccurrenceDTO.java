package com.codegym.appticket.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOccurrenceDTO {

    private Long id;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endTime;

    // Flattened Location Info
    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String provinceCity;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String wardCommune;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String addressDetail;

    private String mapLink;
}
