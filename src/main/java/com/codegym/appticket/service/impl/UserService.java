package com.codegym.appticket.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.codegym.appticket.dto.user.UserDTO;
import com.codegym.appticket.dto.user.UserCreateDTO;
import com.codegym.appticket.dto.user.UserUpdateDTO;
import com.codegym.appticket.dto.user.UserSearchDTO;
import com.codegym.appticket.dto.user.UserStatsDTO;
import com.codegym.appticket.dto.user.UserDetailDTO;
import com.codegym.appticket.dto.user.UserLockHistoryDTO;
import com.codegym.appticket.dto.user.UserProfileDTO;
import com.codegym.appticket.entity.Role;
import com.codegym.appticket.entity.User;
import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.entity.BookingStatus;
import com.codegym.appticket.entity.Event;
import com.codegym.appticket.entity.UserLockHistory;
import com.codegym.appticket.repository.IBookingDetailRepository;
import com.codegym.appticket.repository.IBookingRepository;
import com.codegym.appticket.repository.IEventRepository;
import com.codegym.appticket.repository.IRoleRepository;
import com.codegym.appticket.repository.IUserRepository;
import com.codegym.appticket.repository.IUserLockHistoryRepository;
import com.codegym.appticket.service.IUserService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codegym.appticket.dto.auth.RegisterDTO;
import com.codegym.appticket.entity.AuthenticationProvider;
import jakarta.mail.MessagingException;
import java.util.Random;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements IUserService {

    private final IUserRepository IUserRepository;
    private final IRoleRepository IRoleRepository;
    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    private final IBookingRepository IBookingRepository;
    private final IBookingDetailRepository IBookingDetailRepository;
    private final IEventRepository IEventRepository;

    // Password mặc định khi admin tạo user
    private static final String DEFAULT_PASSWORD = "User@123";

    // ==================== IService Methods ====================

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return IUserRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .map(this::toUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findById(Long id) {
        User user = IUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));
        return toUserDTO(user);
    }

    @Override
    public UserDTO save(UserDTO dto) {
        User user;
        if (dto.getId() != null) {
            user = IUserRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + dto.getId()));
            user.setFullName(dto.getFullName());
            user.setPhoneNumber(dto.getPhoneNumber());
            user.setEnabled(dto.getEnabled());
        } else {
            user = new User();
            user.setFullName(dto.getFullName());
            user.setEmail(dto.getEmail());
            user.setPhoneNumber(dto.getPhoneNumber());
            user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            user.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
            user.setIsDeleted(false);
        }
        User savedUser = IUserRepository.save(user);
        return toUserDTO(savedUser);
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException("Use deleteUser(Long id, String reason) instead. Reason is required.");
    }

    @Override
    public void deleteUser(Long id, String reason) {
        User user = IUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));

        if (user.getIsBlocked() == null || !user.getIsBlocked()) {
            throw new RuntimeException("Người dùng phải bị KHÓA trước khi có thể Xóa!");
        }

        user.setIsDeleted(true);
        user.setDeleteReason(reason);
        IUserRepository.save(user);

        // Send email notification
        try {
            emailService.sendDeleteNotification(user.getEmail(), user.getFullName(), reason);
        } catch (Exception e) {
            System.err.println("Error sending delete notification: " + e.getMessage());
        }
    }

    // ==================== IUserService Methods ====================

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> searchUsers(UserSearchDTO searchDTO) {
        Pageable pageable = PageRequest.of(
                searchDTO.getPage() != null ? searchDTO.getPage() : 0,
                searchDTO.getSize() != null ? searchDTO.getSize() : 10,
                Sort.by(Sort.Direction.DESC, "createdDate"));

        Page<User> userPage = IUserRepository.searchUsers(
                searchDTO.getKeyword(),
                searchDTO.getRoleId(),
                searchDTO.getStatus(),
                pageable);

        return userPage.map(this::toUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsDTO getStats() {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0);

        return new UserStatsDTO(
                IUserRepository.countTotalUsers(),
                IUserRepository.countActiveUsers(),
                IUserRepository.countBlockedUsers(),
                IUserRepository.countNewUsersThisMonth(startOfMonth));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailDTO getUserDetail(Long id) {
        User user = IUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));

        UserDetailDTO dto = new UserDetailDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setEnabled(user.getEnabled());
        dto.setIsBlocked(user.getIsBlocked());
        dto.setIsDeleted(user.getIsDeleted());
        dto.setRoleNames(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        dto.setCreatedDate(user.getCreatedDate());
        dto.setLastModifiedDate(user.getLastModifiedDate());
        dto.setLockReason(user.getLockReason());

        // Populate stats using repositories
        dto.setTicketCount(IBookingDetailRepository.countTicketsByUserId(id));
        dto.setEventCount(IEventRepository.countByCreatedBy(user));

        BigDecimal totalSpent = IBookingDetailRepository.sumTotalSpentByUserId(id);
        if (totalSpent != null) {
            dto.setTotalSpent(String.format("%,.0fđ", totalSpent));
        } else {
            dto.setTotalSpent("0đ");
        }

        // Populate activities
        List<UserDetailDTO.ActivityDTO> activities = new ArrayList<>();

        // 1. Get recent bookings (limit 10)
        Pageable limit10 = PageRequest.of(0, 10);
        List<Booking> recentBookings = IBookingRepository.findByUserOrderByBookingTimeDesc(user, limit10);
        if (recentBookings != null) {
            for (Booking booking : recentBookings) {
                String description;
                try {
                    List<com.codegym.appticket.entity.BookingDetail> details = IBookingDetailRepository
                            .findByBooking(booking);
                    if (details.isEmpty()) {
                        description = "Đặt vé #" + booking.getId();
                    } else {
                        String eventName = details.get(0).getTicketType().getEventOccurrence().getEvent().getTitle();
                        String ticketInfo = details.stream()
                                .map(d -> d.getQuantity() + " "
                                        + d.getTicketType().getEventOccurrence().getEvent().getTitle() + " - "
                                        + d.getTicketType().getName())
                                .collect(Collectors.joining(", "));
                        description = "Đặt vé " + eventName + " (" + ticketInfo + ")";
                    }
                } catch (Exception e) {
                    description = "Đặt vé #" + booking.getId();
                }

                activities.add(new UserDetailDTO.ActivityDTO(
                        "BOOKING",
                        description + " - "
                                + (booking.getStatus() == BookingStatus.SUCCESS ? "Thành công" : "Đang xử lý"),
                        booking.getBookingTime()));
            }
        }

        // 2. Get recent events created (limit 5)
        Pageable limit5 = PageRequest.of(0, 5);
        List<Event> recentEvents = IEventRepository.findByCreatedByOrderByCreatedDateDesc(user, limit5);
        if (recentEvents != null) {
            for (Event event : recentEvents) {
                activities.add(new UserDetailDTO.ActivityDTO(
                        "EVENT_CREATED",
                        "Tạo sự kiện: " + event.getTitle(),
                        event.getCreatedDate()));
            }
        }

        // 3. Sort by date desc
        activities.sort((a, b) -> {
            if (b.getTimestamp() == null || a.getTimestamp() == null)
                return 0;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        // 4. Limit total activities to 10
        if (activities.size() > 10) {
            activities = activities.subList(0, 10);
        }

        dto.setActivities(activities);

        // Populate Lock History
        List<UserLockHistory> historyEntities = userLockHistoryRepository.findByUserOrderByTimestampDesc(user);
        List<UserLockHistoryDTO> historyDTOs = historyEntities.stream()
                .map(h -> new UserLockHistoryDTO(h.getActionType(), h.getReason(), h.getTimestamp(), h.getCreatedBy()))
                .collect(Collectors.toList());
        dto.setLockHistory(historyDTOs);

        return dto;
    }

    @Override
    public UserDTO createUser(UserCreateDTO dto) {
        // Kiểm tra email đã tồn tại
        if (IUserRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng: " + dto.getEmail());
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        user.setIsDeleted(false);
        user.setAuthProvider(AuthenticationProvider.LOCAL);

        // Set role
        if (dto.getRoleId() != null) {
            Role role = IRoleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role với ID: " + dto.getRoleId()));

            if (role.getName().equals("ADMIN")) {
                throw new RuntimeException("Không được phép tạo tài khoản quản trị viên hệ thống");
            }

            Set<Role> roles = new HashSet<>();
            roles.add(role);
            user.setRoles(roles);
        } else {
            // Default role là USER
            Role userRole = IRoleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER không tồn tại"));
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            user.setRoles(roles);
        }

        User savedUser = IUserRepository.save(user);

        // Gửi email thông báo
        try {
            emailService.sendAccountCreatedEmail(savedUser.getEmail(), savedUser.getFullName(), DEFAULT_PASSWORD);
        } catch (MessagingException | UnsupportedEncodingException e) {
            // Log lỗi nhưng không rollback transaction tạo user
            System.err.println("Lỗi gửi email tạo tài khoản: " + e.getMessage());
        }

        return toUserDTO(savedUser);
    }

    @Override
    public UserDTO updateUser(UserUpdateDTO dto) {
        User user = IUserRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + dto.getId()));

        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());

        // Cập nhật email nếu thay đổi
        // Không cho phép cập nhật email
        // if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
        // if (IUserRepository.existsByEmail(dto.getEmail())) {
        // throw new RuntimeException("Email đã được sử dụng: " + dto.getEmail());
        // }
        // user.setEmail(dto.getEmail());
        // }

        // Removed password update logic per requirements

        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }

        // Update role nếu có
        if (dto.getRoleId() != null) {
            Role role = IRoleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role với ID: " + dto.getRoleId()));

            if (role.getName().equals("ADMIN")) {
                throw new RuntimeException("Không được phép gán quyền quản trị viên hệ thống");
            }

            Set<Role> roles = new HashSet<>();
            roles.add(role);
            user.setRoles(roles);
        }

        User savedUser = IUserRepository.save(user);
        return toUserDTO(savedUser);
    }

    @Autowired
    private IUserLockHistoryRepository userLockHistoryRepository;

    @Override
    public void lockUser(Long id, String reason) {
        User user = IUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));

        user.setIsBlocked(true);
        user.setLockedAt(LocalDateTime.now());
        user.setLockReason(reason);
        IUserRepository.save(user);

        // Log history
        UserLockHistory history = new UserLockHistory();
        history.setUser(user);
        history.setActionType("LOCK");
        history.setReason(reason);
        history.setTimestamp(LocalDateTime.now());
        history.setCreatedBy("ADMIN"); // TODO: Get logged in user
        userLockHistoryRepository.save(history);

        try {
            emailService.sendLockNotification(user.getEmail(), user.getFullName(), reason);
        } catch (Exception e) {
            System.err.println("Error sending lock notification: " + e.getMessage());
        }
    }

    @Override
    public void unlockUser(Long id, String reason) {
        User user = IUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));

        user.setIsBlocked(false);
        user.setLockedAt(null);
        user.setUnlockReason(reason);
        // Clear lock reason? Maybe keep history? Requirements just say add reason.
        // Keeping it is safer for history, or we overwrite new action reason.

        IUserRepository.save(user);

        // Log history
        UserLockHistory history = new UserLockHistory();
        history.setUser(user);
        history.setActionType("UNLOCK");
        history.setReason(reason);
        history.setTimestamp(LocalDateTime.now());
        history.setCreatedBy("ADMIN"); // TODO: Get logged in user
        userLockHistoryRepository.save(history);

        try {
            emailService.sendUnlockNotification(user.getEmail(), user.getFullName(), reason);
        } catch (Exception e) {
            System.err.println("Error sending unlock notification: " + e.getMessage());
        }
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        User user = IUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));

        user.setPassword(passwordEncoder.encode(newPassword));
        IUserRepository.save(user);
    }

    @Override
    public UserUpdateDTO getUserForUpdate(Long id) {
        User user = IUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setEnabled(user.getEnabled());
        dto.setCreatedDate(user.getCreatedDate());

        // Lấy role đầu tiên để hiển thị trên form update (vì UI dùng single select)
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            dto.setRoleId(user.getRoles().iterator().next().getId());
        }

        return dto;
    }

    @Override
    public boolean existsByEmail(String email) {
        return IUserRepository.existsByEmail(email);
    }

    // ==================== Registration & OTP Methods ====================

    @Transactional
    public void registerUser(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }
        if (IUserRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setAuthProvider(AuthenticationProvider.LOCAL);
        user.setCreatedDate(LocalDateTime.now());

        // OTP Logic
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setEnabled(false); // Wait for verification

        Role role = IRoleRepository.findByName("USER").orElseThrow(() -> new RuntimeException("Role USER not found"));
        user.setRoles(new HashSet<>(java.util.Collections.singletonList(role)));

        IUserRepository.save(user);

        try {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Lỗi gửi mail xác thực: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otp) {
        User user = IUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (user.getEnabled()) {
            return true; // Already verified
        }

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new RuntimeException("Mã OTP không chính xác");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn");
        }

        user.setEnabled(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        IUserRepository.save(user);
        return true;
    }

    // ==================== Helper Methods ====================

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setEnabled(user.getEnabled());
        dto.setIsBlocked(user.getIsBlocked());
        dto.setIsDeleted(user.getIsDeleted());
        dto.setRoleNames(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        dto.setCreatedDate(user.getCreatedDate());
        return dto;
    }
    // ==================== Forgot Password Methods ====================

    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        User user = IUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

        // Generate OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        IUserRepository.save(user);

        try {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Lỗi gửi mail xác thực: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyPasswordResetOtp(String email, String otp) {
        User user = IUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new RuntimeException("Mã OTP không chính xác");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn");
        }

        return true;
    }

    @Override
    @Transactional
    public void updatePassword(String email, String newPassword) {
        User user = IUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        user.setPassword(passwordEncoder.encode(newPassword));
        // Clear OTP after successful reset
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        IUserRepository.save(user);
    }

    @Override
    public void updateProfile(Long userId, UserProfileDTO dto) {
        User user = IUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + userId));

        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());

        // Nếu email thay đổi, kiểm tra trùng lặp
        // Không cho phép cập nhật email
        // if (!user.getEmail().equals(dto.getEmail())) {
        // if (IUserRepository.existsByEmail(dto.getEmail())) {
        // throw new RuntimeException("Email đã được sử dụng: " + dto.getEmail());
        // }
        // user.setEmail(dto.getEmail());
        // // TODO: Có thể yêu cầu verify lại email nếu cần
        // }

        IUserRepository.save(user);
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        User user = IUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));
        return toUserDTO(user);
    }

    @Override
    public List<Role> getManageableRoles() {
        return IRoleRepository.findAll().stream()
                .filter(r -> !r.getName().equals("ADMIN"))
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkPassword(Long userId, String rawPassword) {
        User user = IUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}
