package com.codegym.appticket.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho form cập nhật user
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {

    @NotNull(message = "ID không được để trống")
    private Long id;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2-100 ký tự")
    private String fullName;

    @Pattern(regexp = "^(0[0-9]{9})?$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    private Long roleId;
    
    // UI không gửi enabled nữa, nhưng giữ field này lại để backward compat hoặc API usage
    private Boolean enabled;

    // Field này được giữ lại để tránh lỗi biên dịch (hidden usage?), nhưng không xử lý logic
    private String password;

    @NotBlank(message = "Email không được để trống")
    @jakarta.validation.constraints.Email(message = "Email không hợp lệ")
    private String email;

    // Fields chỉ để hiển thị (readonly)
    private java.time.LocalDateTime createdDate;
}
