package com.codegym.appticket.service;

import com.codegym.appticket.dto.user.UserDTO;
import com.codegym.appticket.dto.user.*;
import org.springframework.data.domain.Page;

/**
 * Service interface cho quản lý User
 * Extends IService với các method CRUD cơ bản
 */
public interface IUserService extends IService<UserDTO> {

    /**
     * Tìm kiếm users với keyword, filter và phân trang
     */
    Page<UserDTO> searchUsers(UserSearchDTO searchDTO);

    /**
     * Lấy thống kê users cho dashboard
     */
    UserStatsDTO getStats();

    /**
     * Lấy chi tiết user theo ID (bao gồm stats và activity)
     */
    UserDetailDTO getUserDetail(Long id);

    /**
     * Tạo user mới với password mặc định
     */
    UserDTO createUser(UserCreateDTO dto);

    /**
     * Cập nhật thông tin user
     */
    UserDTO updateUser(UserUpdateDTO dto);

    /**
     * Khóa/Mở khóa/Xóa tài khoản với lý do
     */
    void lockUser(Long id, String reason);
    void unlockUser(Long id, String reason);
    void deleteUser(Long id, String reason);

    // Registration & OTP
    void registerUser(com.codegym.appticket.dto.auth.RegisterDTO dto);
    boolean verifyOtp(String email, String otp);

    // ==================== IUserService Specific Methods ====================user
    /**
     * Reset mật khẩu cho user
     */
    void resetPassword(Long id, String newPassword);

    /**
     * Lấy thông tin user để cập nhật (cho form edit)
     */
    /**
     * Lấy thông tin user để cập nhật (cho form edit)
     */
    UserUpdateDTO getUserForUpdate(Long id);

    /**
     * Gửi OTP quên mật khẩu
     */
    void initiatePasswordReset(String email);

    /**
     * Xác thực OTP quên mật khẩu
     */
    boolean verifyPasswordResetOtp(String email, String otp);

    /**
     * Đặt lại mật khẩu (cần có OTP đã xác thực trong session hoặc logic tương tự)
     * Tuy nhiên, ở bước này ta sẽ trust là controller đã verify OTP.
     * Hoặc an toàn hơn: truyển cả OTP vào để verify lần cuối trước khi đổi pass.
     */
    void updatePassword(String email, String newPassword);

    /**
     * Cập nhật thông tin profile người dùng
     */
    void updateProfile(Long userId, UserProfileDTO dto);

    boolean existsByEmail(String email);

    UserDTO getUserByEmail(String email);

    java.util.List<com.codegym.appticket.entity.Role> getManageableRoles();

    boolean checkPassword(Long userId, String rawPassword);
}
