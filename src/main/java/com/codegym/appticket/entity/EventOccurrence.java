package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.ArrayList;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_occurrences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Builder.Default
    @OneToMany(mappedBy = "eventOccurrence", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketType> ticketTypes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    public String getFullLocation() {
        if (location != null) {
            StringBuilder fullAddress = new StringBuilder(location.getAddressDetail());
            if (location.getWard() != null) {
                fullAddress.append(", ").append(location.getWard().getName());
                if (location.getWard().getProvince() != null) {
                    fullAddress.append(", ").append(location.getWard().getProvince().getName());
                }
            }
            return fullAddress.toString();
        }
        return "Địa điểm chưa xác định";
    }
}
