package com.codegym.appticket.dto.event;

import com.codegym.appticket.entity.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMediaDTO {

    private Long id;

    @NotBlank(message = "URL media không được để trống")
    private String mediaUrl;

    @NotNull(message = "Loại media không được để trống")
    private MediaType mediaType;

    @NotNull(message = "Mục đích sử dụng media không được để trống")
    private com.codegym.appticket.entity.MediaPurpose mediaPurpose;

    @Builder.Default
    private Boolean isThumbnail = false;
}
