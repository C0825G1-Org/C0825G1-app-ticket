package com.codegym.appticket.service.impl;

import com.codegym.appticket.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserCleanupService {

    @Autowired
    private IUserRepository userRepository;

    // Run every 5 minutes (300,000 ms)
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void removeUnverifiedUsers() {
        LocalDateTime now = LocalDateTime.now();
        userRepository.deleteByEnabledFalseAndOtpExpiryBefore(now);
    }
}
