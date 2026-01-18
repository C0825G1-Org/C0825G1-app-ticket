package com.codegym.appticket.controller;

import com.codegym.appticket.dto.user.UserDetailDTO;
import com.codegym.appticket.dto.user.UserProfileDTO;
import com.codegym.appticket.service.IUserService;
import com.codegym.appticket.service.impl.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.codegym.appticket.dto.user.UserInfoUserDetails;

@Controller
                        @RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final IUserService userService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserInfoUserDetails) {
                return ((UserInfoUserDetails) principal).getUser().getId();
            } else if (principal instanceof com.codegym.appticket.config.CustomOAuth2User) {
                String email = ((com.codegym.appticket.config.CustomOAuth2User) principal).getEmail();
                // Fetch user ID by email
                return userService.getUserByEmail(email).getId();
            }
        }
        throw new RuntimeException("User not authenticated or unknown principal type");
    }
    
    // Helper to get email from context (safer than trusting client)
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserInfoUserDetails) {
                return ((UserInfoUserDetails) principal).getUsername();
            } else if (principal instanceof com.codegym.appticket.config.CustomOAuth2User) {
                return ((com.codegym.appticket.config.CustomOAuth2User) principal).getEmail();
            }
        }
        return auth.getName(); // Fallback
    }

    @GetMapping
    public String viewProfile(Model model) {
        try {
            Long userId = getCurrentUserId();
            UserDetailDTO userDetail = userService.getUserDetail(userId);
            model.addAttribute("user", userDetail);
            
            // Map to UserProfileDTO for form binding
            UserProfileDTO profileDTO = new UserProfileDTO();
            profileDTO.setFullName(userDetail.getFullName());
            profileDTO.setEmail(userDetail.getEmail());
            profileDTO.setPhoneNumber(userDetail.getPhoneNumber());
            model.addAttribute("userProfileDTO", profileDTO);
            
            return "profile/index";
        } catch (Exception e) {
            return "redirect:/login";
        }
    }

    @PostMapping("/update")
    public String updateProfile(@jakarta.validation.Valid @ModelAttribute UserProfileDTO dto, 
                              org.springframework.validation.BindingResult bindingResult, 
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            // Reload user details for the view (header, stats, history)
            try {
                Long userId = getCurrentUserId();
                UserDetailDTO userDetail = userService.getUserDetail(userId);
                model.addAttribute("user", userDetail);
                // The invalid dto and bindingResult are automatically in the model
                return "profile/index";
            } catch (Exception e) {
                 return "redirect:/login"; // Should not happen for logged in user
            }
        }

        try {
            Long userId = getCurrentUserId();
            userService.updateProfile(userId, dto);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/password/otp")
    @ResponseBody // Ajax call
    public org.springframework.http.ResponseEntity<?> sendPasswordOtp(@RequestParam("currentPassword") String currentPassword,
                                                                    @RequestParam("newPassword") String newPassword,
                                                                    @RequestParam("confirmPassword") String confirmPassword) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                return org.springframework.http.ResponseEntity.badRequest().body("Mật khẩu xác nhận không khớp!");
            }
            
            Long userId = getCurrentUserId();
            if (!userService.checkPassword(userId, currentPassword)) {
                return org.springframework.http.ResponseEntity.badRequest().body("Mật khẩu hiện tại không chính xác!");
            }

            String email = getCurrentUserEmail();
            userService.initiatePasswordReset(email);
            return org.springframework.http.ResponseEntity.ok("SUCCESS");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().body("ERROR: " + e.getMessage());
        }
    }

    @PostMapping("/password/change")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> changePassword(@RequestParam("otp") String otp, 
                               @RequestParam("newPassword") String newPassword,
                               @RequestParam("confirmPassword") String confirmPassword) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                 return org.springframework.http.ResponseEntity.badRequest().body("Mật khẩu xác nhận không khớp!");
            }
            
            String email = getCurrentUserEmail();
            
            // Verify OTP
            if (userService.verifyPasswordResetOtp(email, otp)) {
                // Update Password
                userService.updatePassword(email, newPassword);
                return org.springframework.http.ResponseEntity.ok("SUCCESS");
            } else {
                 return org.springframework.http.ResponseEntity.badRequest().body("Mã OTP không chính xác hoặc đã hết hạn!");
            }
            
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().body("Lỗi đổi mật khẩu: " + e.getMessage());
        }
    }
}
