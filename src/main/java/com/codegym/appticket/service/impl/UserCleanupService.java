package com.codegym.appticket.service.impl;

import com.codegym.appticket.repository.IUserRepository;
import com.codegym.appticket.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    @Autowired
    private EmailService emailService;

    // Run every day at midnight? Or fixed rate?
    // User requested "automating... after 30 days". A daily check is sufficient.
    // Cron: At 00:00:00am every day
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void autoDeleteLockedUsers() {
        // 30 days ago
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        List<User> usersToDelete = userRepository.findAllByIsBlockedTrueAndLockedAtBefore(threshold);

        for (User user : usersToDelete) {
            // Logic: Soft delete
            user.setIsDeleted(true);
            user.setDeleteReason("Auto-deleted after 30 days of being locked without resolution.");
            // Notify
            try {
                emailService.sendAutoDeleteNotification(user.getEmail(), user.getFullName());
            } catch (Exception e) {
                System.err.println("Failed to send auto-delete email to " + user.getEmail() + ": " + e.getMessage());
            }
            userRepository.save(user);
        }
    }
}
