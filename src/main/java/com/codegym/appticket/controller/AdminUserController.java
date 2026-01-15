package com.codegym.appticket.controller;

import com.codegym.appticket.dto.user.UserDTO;
import com.codegym.appticket.dto.user.*;
import com.codegym.appticket.entity.Role;
import com.codegym.appticket.repository.IRoleRepository;
import com.codegym.appticket.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final IUserService userService;
    private final IRoleRepository IRoleRepository;

    /**
     * Danh sách users với search, filter, pagination
     */
    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Model model
    ) {
        UserSearchDTO searchDTO = new UserSearchDTO(keyword, roleId, status, page, size);
        Page<UserDTO> userPage = userService.searchUsers(searchDTO);
        UserStatsDTO stats = userService.getStats();
        List<Role> roles = IRoleRepository.findAll();

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page + 1);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalUsers", stats.getTotalUsers());
        model.addAttribute("activeUsers", stats.getActiveUsers());
        model.addAttribute("lockedUsers", stats.getLockedUsers());
        model.addAttribute("newUsers", stats.getNewUsersThisMonth());
        model.addAttribute("pageSize", size);
        
        // Search params để giữ lại khi phân trang
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("roles", roles);
        model.addAttribute("currentPageNav", "users");

        return "admin/user/list";
    }

    /**
     * Xem chi tiết user
     */
    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            UserDetailDTO user = userService.getUserDetail(id);
            
            // Không cho xem user đã bị xóa
            if (user.getIsDeleted() != null && user.getIsDeleted()) {
                redirectAttributes.addFlashAttribute("error", "Người dùng này đã bị xóa!");
                return "redirect:/admin/users";
            }
            
            model.addAttribute("user", user);
            model.addAttribute("currentPageNav", "users");
            return "admin/user/detail";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * Form tạo user mới
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new UserCreateDTO());
        model.addAttribute("roles", IRoleRepository.findAll());
        model.addAttribute("currentPageNav", "users");
        return "admin/user/create";
    }

    /**
     * Submit tạo user mới
     */
    @PostMapping("/create")
    public String createUser(
            @Valid @ModelAttribute("user") UserCreateDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", IRoleRepository.findAll());
            model.addAttribute("currentPageNav", "users");
            return "admin/user/create";
        }

        try {
            userService.createUser(dto);
            redirectAttributes.addFlashAttribute("success", "Tạo người dùng thành công! Mật khẩu mặc định: User@123");
            return "redirect:/admin/users";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", IRoleRepository.findAll());
            model.addAttribute("currentPageNav", "users");
            return "admin/user/create";
        }
    }

    /**
     * Form sửa user
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        UserUpdateDTO dto = userService.getUserForUpdate(id);
        UserDetailDTO userDetail = userService.getUserDetail(id);
        
        model.addAttribute("user", dto);
        model.addAttribute("userDetail", userDetail);
        model.addAttribute("roles", IRoleRepository.findAll());
        model.addAttribute("currentPageNav", "users");
        return "admin/user/edit";
    }

    /**
     * Submit sửa user
     */
    @PostMapping("/{id}/edit")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute("user") UserUpdateDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        dto.setId(id);
        
        if (bindingResult.hasErrors()) {
            UserDetailDTO userDetail = userService.getUserDetail(id);
            model.addAttribute("userDetail", userDetail);
            model.addAttribute("roles", IRoleRepository.findAll());
            model.addAttribute("currentPageNav", "users");
            return "admin/user/edit";
        }

        try {
            userService.updateUser(dto);
            redirectAttributes.addFlashAttribute("success", "Cập nhật người dùng thành công!");
            return "redirect:/admin/users";
        } catch (RuntimeException e) {
            UserDetailDTO userDetail = userService.getUserDetail(id);
            model.addAttribute("userDetail", userDetail);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", IRoleRepository.findAll());
            model.addAttribute("currentPageNav", "users");
            return "admin/user/edit";
        }
    }

    /**
     * Soft delete user
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa người dùng!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Khóa/Mở khóa tài khoản
     */
    @PostMapping("/{id}/toggle-lock")
    public String toggleLock(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleLock(id);
            redirectAttributes.addFlashAttribute("success", "Đã thay đổi trạng thái tài khoản!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Reset mật khẩu
     */
    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success", "Đã đặt lại mật khẩu thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id + "/edit";
    }
}
