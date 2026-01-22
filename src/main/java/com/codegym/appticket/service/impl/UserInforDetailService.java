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
        
        // Thử tìm user mà không kèm điều kiện isDeleted trước để debug
        java.util.Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + email);
        }

        User user = userOptional.get();

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new UsernameNotFoundException("User has been deleted");
        }

        UserInfoUserDetails userInfoUserDetails = new UserInfoUserDetails(user);
        return userInfoUserDetails;
    }
}
