package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_lock_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "action_type", nullable = false)
    private String actionType; // "LOCK" or "UNLOCK" or "AUTO_DELETE"

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_by")
    private String createdBy; // e.g., "admin@example.com" or "SYSTEM"
}
