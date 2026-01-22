package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_cancellation_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventCancellationHistory extends Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Người thực hiện (Admin hoặc Organizer)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status; // REJECTED hoặc CANCELLED

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;
}
