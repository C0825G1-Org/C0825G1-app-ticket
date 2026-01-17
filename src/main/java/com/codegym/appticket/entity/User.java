package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends Parent {

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @Column(name = "auth_provider")
    private AuthenticationProvider authProvider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expiry")
    private java.time.LocalDateTime otpExpiry;

    @Column(name = "enabled")
    private Boolean enabled = false; // Default false for new users (wait for OTP)

    @Column(name = "locked_at")
    private java.time.LocalDateTime lockedAt;

    @Column(name = "lock_reason", columnDefinition = "TEXT")
    private String lockReason;

    @Column(name = "unlock_reason", columnDefinition = "TEXT")
    private String unlockReason;

    @Column(name = "delete_reason", columnDefinition = "TEXT")
    private String deleteReason;

    @ManyToMany
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
