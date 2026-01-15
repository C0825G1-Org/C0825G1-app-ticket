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
        User user = userRepository.findByEmailAndNotDeleted(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        UserInfoUserDetails userInfoUserDetails = new UserInfoUserDetails(user);
        return userInfoUserDetails;
    }
}
