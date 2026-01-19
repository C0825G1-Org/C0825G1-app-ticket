package com.codegym.appticket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.codegym.appticket.dto.auth.RegisterDTO;
import com.codegym.appticket.service.IUserService;
import com.codegym.appticket.repository.IUserRepository;

@Controller
public class AuthController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IUserRepository userRepository;

    @GetMapping("/login")
    public String login() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                return "redirect:/admin/users";
            }
            return "redirect:/profile";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
                           BindingResult bindingResult,
                           Model model,
                           HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        
        try {
            userService.registerUser(registerDTO);
            session.setAttribute("verifyEmail", registerDTO.getEmail());
            return "redirect:/verify-otp";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyForm(Model model, HttpSession session) {
        String email = (String) session.getAttribute("verifyEmail");
        if (email == null) {
            return "redirect:/register";
        }
        model.addAttribute("email", email);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("verifyEmail");
        if (email == null) {
            return "redirect:/register";
        }

        try {
            userService.verifyOtp(email, otp);
            session.removeAttribute("verifyEmail");
            
            // Auto login
             com.codegym.appticket.entity.User user = userRepository.findByEmail(email).orElse(null);
             if (user != null) {
                 org.springframework.security.core.userdetails.UserDetails userDetails = 
                     org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                         .password(user.getPassword())
                         .authorities(user.getRoles().stream().map(r -> r.getName()).toArray(String[]::new))
                         .build();
                 
                 org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication = 
                     new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                 
                 org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
             }

            return "redirect:/";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/verify-otp";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }

    // ==================== Forgot Password Endpoints ====================

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            userService.initiatePasswordReset(email);
            session.setAttribute("resetEmail", email);
            return "redirect:/verify-forgot-password";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/verify-forgot-password")
    public String showVerifyForgotPasswordForm(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("email", email);
        return "auth/verify-forgot-password";
    }

    @PostMapping("/verify-forgot-password")
    public String verifyForgotPasswordOtp(@RequestParam String otp, HttpSession session, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            return "redirect:/forgot-password";
        }

        try {
            userService.verifyPasswordResetOtp(email, otp);
            session.setAttribute("resetVerified", true); // Mark as verified
            return "redirect:/reset-password";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/verify-forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(HttpSession session) {
        String email = (String) session.getAttribute("resetEmail");
        Boolean verified = (Boolean) session.getAttribute("resetVerified");

        if (email == null || verified == null || !verified) {
            return "redirect:/forgot-password";
        }
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String password, @RequestParam String confirmPassword, HttpSession session, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("resetEmail");
        Boolean verified = (Boolean) session.getAttribute("resetVerified");

        if (email == null || verified == null || !verified) {
            return "redirect:/forgot-password";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/reset-password";
        }

        try {
            userService.updatePassword(email, password);
            session.removeAttribute("resetEmail");
            session.removeAttribute("resetVerified");
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password";
        }
    }
}
