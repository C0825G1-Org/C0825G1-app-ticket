package com.codegym.appticket.dto.event;

import com.codegym.appticket.entity.EventStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventUpdateDTO {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotBlank(message = "Địa điểm không được để trống")
    @Size(max = 255, message = "Địa điểm không được vượt quá 255 ký tự")
    private String location;

    @NotNull(message = "Danh mục sự kiện không được để trống")
    private Long categoryId;

    @NotNull(message = "Trạng thái không được để trống")
    private EventStatus status;

    @Valid
    @Builder.Default
    private List<EventOccurrenceDTO> eventOccurrences = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<EventMediaDTO> eventMedias = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<TicketTypeDTO> ticketTypes = new ArrayList<>();

    // Media URLs (Uploaded from Frontend)
    private String bannerUrl;
    private String logoUrl;
    private String ticketMapUrl;
    private List<String> galleryUrls;
}
