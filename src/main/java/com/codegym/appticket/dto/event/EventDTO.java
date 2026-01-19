package com.codegym.appticket.dto.event;

import com.codegym.appticket.entity.EventStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {
    private Long id;
    private String title;
    private String description;

    private Long categoryId;
    private String categoryName;
    private Long createdById;
    private String createdByName;
    private EventStatus status;
    private LocalDateTime createdAt;
    private Long organizerId;
    private String organizerName;

    @Builder.Default
    private List<EventOccurrenceDTO> eventOccurrences = new ArrayList<>();

    @Builder.Default
    private List<EventMediaDTO> eventMedias = new ArrayList<>();

    @Builder.Default
    private List<TicketTypeDTO> ticketTypes = new ArrayList<>();
}
