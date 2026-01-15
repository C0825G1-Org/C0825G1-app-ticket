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
     * Khóa/Mở khóa tài khoản
     */
    void toggleLock(Long id);

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

    boolean existsByEmail(String email);
}
