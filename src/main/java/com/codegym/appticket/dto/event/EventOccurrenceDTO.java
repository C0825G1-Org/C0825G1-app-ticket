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
    // Codes are required for mapping
    @NotBlank(message = "Vui lòng chọn Tỉnh/Thành phố")
    private String provinceCode;
    private String provinceName; // To save if new

    private String districtCode; // Frontend use only
    private String districtName; // Frontend use only

    @NotBlank(message = "Vui lòng chọn Phường/Xã")
    private String wardCode;
    private String wardName; // To save if new

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String addressDetail;

    private String mapLink;
}
