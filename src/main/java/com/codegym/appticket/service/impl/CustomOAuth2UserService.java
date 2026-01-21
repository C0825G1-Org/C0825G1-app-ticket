package com.codegym.appticket.service.impl;

import com.codegym.appticket.config.CustomOAuth2User;
import com.codegym.appticket.entity.AuthenticationProvider;
import com.codegym.appticket.entity.Role;
import com.codegym.appticket.entity.User;
import com.codegym.appticket.repository.IRoleRepository;
import com.codegym.appticket.repository.IUserRepository;
import jakarta.persistence.NoResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        CustomOAuth2User customUser = new CustomOAuth2User(user);

//        SecurityContext context = SecurityContextHolder.getContext();
//        context.setAuthentication();
        processOAuth2PostLogin(customUser);

        return customUser;
    }

    private void processOAuth2PostLogin(CustomOAuth2User oAuth2User) {
        String email = oAuth2User.getEmail();
        Optional<User> existUser = userRepository.findByEmail(email);

        if (existUser.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(oAuth2User.getName());
            newUser.setPassword(""); // Mock password for OAuth2 user
            newUser.setAuthProvider(AuthenticationProvider.GOOGLE);
            newUser.setEnabled(true); // Auto-enable Google users
            newUser.setCreatedDate(LocalDateTime.now());
            
            // Set default role USER
            Role userRole = roleRepository.findByName("USER").orElseThrow(() -> new NoResultException("User not found"));
            if (userRole != null) {
                newUser.setRoles(new HashSet<>(Collections.singletonList(userRole)));
            }
            
            userRepository.save(newUser);
        } else {
             User user = existUser.get();
             
             // Check if blocked or deleted
             if ((user.getIsBlocked() != null && user.getIsBlocked()) || 
                 (user.getIsDeleted() != null && user.getIsDeleted())) {
                 throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("account_locked"), "Tài khoản của bạn đã bị khóa hoặc bị xóa.");
             }

             // Luôn đồng bộ tên từ Google để đảm bảo chính xác nhất
             String googleName = oAuth2User.getName();
             if (googleName != null && !googleName.equals(user.getFullName())) {
                user.setFullName(googleName);
                 userRepository.save(user);
             }
        }
    }
}
