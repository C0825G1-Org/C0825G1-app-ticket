package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest extends Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision")
    private DecisionStatus decision;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
