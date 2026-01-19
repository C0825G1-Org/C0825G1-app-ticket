package com.codegym.appticket.dto.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class EventCreateDTO {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 10, max = 255, message = "Tiêu đề phải từ 10 đến 255 ký tự")
    private String title;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 50, message = "Mô tả phải có ít nhất 50 ký tự")
    private String description;

    @NotBlank(message = "Địa điểm không được để trống")
    @Size(min = 5, max = 255, message = "Địa điểm phải từ 5 đến 255 ký tự")
    private String location;

    @NotNull(message = "Danh mục sự kiện không được để trống")
    private Long categoryId;

    @NotEmpty(message = "Sự kiện phải có ít nhất một xuất tổ chức")
    @Valid
    @Builder.Default
    private List<EventOccurrenceDTO> eventOccurrences = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<TicketTypeDTO> ticketTypes = new ArrayList<>();

    // Media URLs (Uploaded from Frontend)
    private String bannerUrl;
    private String logoUrl;
    private String ticketMapUrl;
    private List<String> galleryUrls;

    private com.codegym.appticket.entity.EventStatus status;

    // Optional: For Admin to assign organizer
    private Long organizerId;
}
