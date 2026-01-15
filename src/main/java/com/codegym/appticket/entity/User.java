package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends Parent {

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

    // Enabled = false đại diện cho tài khoản người dùng đang chờ xác nhận mail
    // (nếu họ xác nhận mail thì mới enabled tài khoản)
    //Nhưng vì chưa phát triển tính năng gửi mail nên tạm thời để true để phục vụ tạo tài khoản user
    // isBlocked = true nghĩa là admin đã khóa tài khoản này
    // User vẫn hiển thị trong danh sách admin nhưng không thể thao tác
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

    @ManyToMany
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
