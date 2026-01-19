package com.codegym.appticket.service.impl;

import com.codegym.appticket.dto.user.UserInfoUserDetails;
import com.codegym.appticket.entity.User;
import com.codegym.appticket.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserInforDetailService implements UserDetailsService {
    @Autowired
    private IUserRepository userRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("DEBUG: Looking for user: " + email);
        
        // Thử tìm user mà không kèm điều kiện isDeleted trước để debug
        java.util.Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            System.out.println("DEBUG: [FAILED] No user found with email: " + email);
            throw new UsernameNotFoundException("User not found: " + email);
        }

        User user = userOptional.get();
        System.out.println("DEBUG: [FOUND] User ID: " + user.getId() + ", Email: " + user.getEmail());
        System.out.println("DEBUG: Password in DB: " + user.getPassword());
        System.out.println("DEBUG: Status - Enabled: " + user.getEnabled() + ", IsDeleted: " + user.getIsDeleted() + ", IsBlocked: " + user.getIsBlocked());

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            System.out.println("DEBUG: [FAILED] User is marked as DELETED");
            throw new UsernameNotFoundException("User has been deleted");
        }

        UserInfoUserDetails userInfoUserDetails = new UserInfoUserDetails(user);
        return userInfoUserDetails;
    }
}
