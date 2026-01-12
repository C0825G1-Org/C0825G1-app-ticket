package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision")
    private DecisionStatus decision;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "decision_time")
    private LocalDateTime decisionTime;

    @PrePersist
    protected void onCreate() {
        this.decisionTime = LocalDateTime.now();
    }
}
