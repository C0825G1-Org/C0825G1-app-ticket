package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event extends Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private EventCategory category;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EventStatus status = EventStatus.PENDING;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventMedia> eventMedias = new ArrayList<>();

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventOccurrence> eventOccurrences = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventCancellationHistory> cancellationHistories = new ArrayList<>();

    public String getLocation() {
        if (eventOccurrences != null && !eventOccurrences.isEmpty()) {
            EventOccurrence occurrence = eventOccurrences.get(0);
            if (occurrence.getLocation() != null) {
                Location loc = occurrence.getLocation();
                StringBuilder fullAddress = new StringBuilder(loc.getAddressDetail());
                if (loc.getWard() != null) {
                    fullAddress.append(", ").append(loc.getWard().getName());
                    if (loc.getWard().getProvince() != null) {
                        fullAddress.append(", ").append(loc.getWard().getProvince().getName());
                    }
                }
                return fullAddress.toString();
            }
        }
        return "Địa điểm chưa xác định";
    }
}
